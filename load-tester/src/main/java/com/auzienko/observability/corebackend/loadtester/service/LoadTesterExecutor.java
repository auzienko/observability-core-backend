package com.auzienko.observability.corebackend.loadtester.service;

import com.auzienko.observability.corebackend.domain.model.HttpRequest;
import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;
import com.auzienko.observability.corebackend.domain.model.Step;
import com.auzienko.observability.corebackend.loadtester.model.RawRequestResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class LoadTesterExecutor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LoadTesterExecutor(
            @Qualifier("loadTestRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void execute(UUID serviceId, LoadTestScenario scenario, Consumer<RawRequestResult> resultConsumer) {
        log.info("Starting load test execution: serviceId={}, virtualUsers={}, duration={}s, runs={}",
                serviceId, scenario.getVirtualUsers(), scenario.getDurationSeconds(), scenario.getRuns());

        validateScenario(scenario);

        ExecutorService executor = createExecutor(scenario.getVirtualUsers());
        try {
            List<Future<?>> futures = submitWorkers(executor, scenario, resultConsumer);
            awaitCompletion(futures);
        } finally {
            shutdownExecutor(executor);
        }

        log.info("Load test execution completed for service {}", serviceId);
    }

    /**
     * Async version that returns immediately.
     */
    public CompletableFuture<Void> executeAsync(UUID serviceId, LoadTestScenario scenario,
                                                Consumer<RawRequestResult> resultConsumer) {
        return CompletableFuture.runAsync(() -> execute(serviceId, scenario, resultConsumer));
    }

    private void validateScenario(LoadTestScenario scenario) {
        if (scenario.getRuns() == null &&
                (scenario.getDurationSeconds() == null || scenario.getDurationSeconds() <= 0)) {
            throw new IllegalArgumentException("Either runs or durationSeconds must be specified");
        }
        if (scenario.getVirtualUsers() == null || scenario.getVirtualUsers() <= 0) {
            throw new IllegalArgumentException("virtualUsers must be greater than 0");
        }
        if (scenario.getSteps() == null || scenario.getSteps().isEmpty()) {
            throw new IllegalArgumentException("scenario must contain at least one step");
        }
    }

    private ExecutorService createExecutor(int virtualUsers) {
        return Executors.newFixedThreadPool(virtualUsers);
    }

    private List<Future<?>> submitWorkers(ExecutorService executor, LoadTestScenario scenario,
                                          Consumer<RawRequestResult> resultConsumer) {
        return IntStream.range(0, scenario.getVirtualUsers())
                .mapToObj(workerId -> executor.submit(() ->
                        runWorker(workerId, scenario, resultConsumer)))
                .collect(Collectors.toUnmodifiableList());
    }

    private void runWorker(int workerId, LoadTestScenario scenario,
                           Consumer<RawRequestResult> resultConsumer) {
        try {
            if (scenario.getRuns() != null) {
                runFixedIterations(workerId, scenario, resultConsumer);
            } else {
                runDurationBased(workerId, scenario, resultConsumer);
            }
        } catch (Exception e) {
            log.error("Worker {} encountered unexpected error", workerId, e);
        }
    }

    private void runFixedIterations(int workerId, LoadTestScenario scenario,
                                    Consumer<RawRequestResult> resultConsumer) {
        UserContext context = new UserContext(workerId);

        for (int iteration = 0; iteration < scenario.getRuns() && !Thread.currentThread().isInterrupted(); iteration++) {
            executeScenarioSteps(workerId, iteration, scenario, context, resultConsumer);
        }
    }

    private void runDurationBased(int workerId, LoadTestScenario scenario,
                                  Consumer<RawRequestResult> resultConsumer) {
        UserContext context = new UserContext(workerId);
        long testEndTime = System.currentTimeMillis() + (scenario.getDurationSeconds() * 1000L);
        int iteration = 0;

        while (System.currentTimeMillis() < testEndTime && !Thread.currentThread().isInterrupted()) {
            executeScenarioSteps(workerId, iteration++, scenario, context, resultConsumer);
        }
    }

    private void executeScenarioSteps(int workerId, int iteration, LoadTestScenario scenario,
                                      UserContext context, Consumer<RawRequestResult> resultConsumer) {
        for (int stepIndex = 0; stepIndex < scenario.getSteps().size(); stepIndex++) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            Step step = scenario.getSteps().get(stepIndex);
            RawRequestResult result = executeStep(workerId, iteration, stepIndex, step, context);

            // Emit result immediately to consumer
            try {
                resultConsumer.accept(result);
            } catch (Exception e) {
                log.error("Consumer failed to process result", e);
            }
        }
    }

    private RawRequestResult executeStep(int workerId, int iteration, int stepIndex,
                                         Step step, UserContext context) {
        HttpRequest request = step.getRequest();
        String url = resolveVariables(request.getUrl(), context);
        HttpMethod method = HttpMethod.valueOf(request.getMethod().toUpperCase());
        HttpEntity<String> requestEntity = prepareRequest(request, context);

        Instant startTime = Instant.now();
        long startNanos = System.nanoTime();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, requestEntity, String.class);
            long durationNanos = System.nanoTime() - startNanos;

            // Extract variables for next steps
            extractVariables(response.getBody(), step.getExtract(), context);

            return RawRequestResult.builder()
                    .workerId(workerId)
                    .iteration(iteration)
                    .stepIndex(stepIndex)
                    .stepName(step.getName())
                    .url(url)
                    .method(method.name())
                    .requestHeaders(extractHeaders(requestEntity.getHeaders()))
                    .requestBody(requestEntity.getBody())
                    .startTime(startTime)
                    .durationNanos(durationNanos)
                    .statusCode(response.getStatusCode().value())
                    .responseHeaders(extractHeaders(response.getHeaders()))
                    .responseBody(response.getBody())
                    .success(true)
                    .build();

        } catch (org.springframework.web.client.ResourceAccessException e) {
            long durationNanos = System.nanoTime() - startNanos;
            return createErrorResult(workerId, iteration, stepIndex, step, url, method,
                    requestEntity, startTime, durationNanos, "CONNECTION_ERROR", e);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            long durationNanos = System.nanoTime() - startNanos;
            return createErrorResult(workerId, iteration, stepIndex, step, url, method,
                    requestEntity, startTime, durationNanos,
                    "HTTP_CLIENT_ERROR", e, e.getStatusCode().value());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            long durationNanos = System.nanoTime() - startNanos;
            return createErrorResult(workerId, iteration, stepIndex, step, url, method,
                    requestEntity, startTime, durationNanos,
                    "HTTP_SERVER_ERROR", e, e.getStatusCode().value());
        } catch (Exception e) {
            long durationNanos = System.nanoTime() - startNanos;
            return createErrorResult(workerId, iteration, stepIndex, step, url, method,
                    requestEntity, startTime, durationNanos, "UNKNOWN_ERROR", e);
        }
    }

    private RawRequestResult createErrorResult(int workerId, int iteration, int stepIndex,
                                               Step step, String url, HttpMethod method,
                                               HttpEntity<String> requestEntity, Instant startTime,
                                               long durationNanos, String errorType, Exception e) {
        return createErrorResult(workerId, iteration, stepIndex, step, url, method,
                requestEntity, startTime, durationNanos, errorType, e, null);
    }

    private RawRequestResult createErrorResult(int workerId, int iteration, int stepIndex,
                                               Step step, String url, HttpMethod method,
                                               HttpEntity<String> requestEntity, Instant startTime,
                                               long durationNanos, String errorType, Exception e,
                                               Integer statusCode) {
        return RawRequestResult.builder()
                .workerId(workerId)
                .iteration(iteration)
                .stepIndex(stepIndex)
                .stepName(step.getName())
                .url(url)
                .method(method.name())
                .requestHeaders(extractHeaders(requestEntity.getHeaders()))
                .requestBody(requestEntity.getBody())
                .startTime(startTime)
                .durationNanos(durationNanos)
                .statusCode(statusCode)
                .success(false)
                .errorType(errorType)
                .errorMessage(e.getMessage())
                .build();
    }

    private Map<String, String> extractHeaders(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        headers.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                result.put(key, String.join(", ", values));
            }
        });
        return result;
    }

    private void awaitCompletion(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                log.error("Worker task failed", e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for workers", e);
                break;
            }
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private HttpEntity<String> prepareRequest(HttpRequest request, UserContext context) {
        HttpHeaders headers = buildHeaders(request, context);
        String body = buildBody(request, context);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders buildHeaders(HttpRequest request, UserContext context) {
        HttpHeaders headers = new HttpHeaders();
        if (request.getHeaders() != null) {
            request.getHeaders().forEach((key, value) ->
                    headers.set(key, resolveVariables(value, context)));
        }
        return headers;
    }

    private String buildBody(HttpRequest request, UserContext context) {
        if (request.getBody() == null || request.getBody().isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> resolvedBody = new HashMap<>();
            request.getBody().forEach((key, value) -> {
                Object resolvedValue = (value instanceof String)
                        ? resolveVariables((String) value, context)
                        : value;
                resolvedBody.put(key, resolvedValue);
            });
            return objectMapper.writeValueAsString(resolvedBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    private String resolveVariables(String input, UserContext context) {
        if (input == null || !input.contains("${")) {
            return input;
        }

        String result = input;

        while (result.contains("${randomUUID}")) {
            result = result.replaceFirst("\\$\\{randomUUID\\}", UUID.randomUUID().toString());
        }
        if (result.contains("${timestamp}")) {
            result = result.replace("${timestamp}", String.valueOf(System.currentTimeMillis()));
        }

        for (Map.Entry<String, Object> entry : context.getVariables().entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        return result;
    }

    private void extractVariables(String responseBody, Map<String, String> extractConfig,
                                  UserContext context) {
        if (extractConfig == null || extractConfig.isEmpty() ||
                responseBody == null || responseBody.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : extractConfig.entrySet()) {
            String variableName = entry.getKey();
            String expression = entry.getValue();

            if (expression.startsWith("jsonpath:")) {
                String jsonPath = expression.substring("jsonpath:".length());
                try {
                    Object extractedValue = JsonPath.read(responseBody, jsonPath);
                    context.setVariable(variableName, extractedValue);
                } catch (Exception e) {
                    log.warn("Failed to extract '{}' with JSONPath '{}': {}",
                            variableName, jsonPath, e.getMessage());
                }
            }
        }
    }

    private static class UserContext {

        @Getter
        private final int workerId;

        @Getter
        private final Map<String, Object> variables = new HashMap<>();

        UserContext(int workerId) {
            this.workerId = workerId;
        }

        void setVariable(String name, Object value) {
            variables.put(name, value);
        }

    }

}
