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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;

@Service
@Slf4j
public class VirtualThreadLoadTestExecutor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VirtualThreadLoadTestExecutor(
            @Qualifier("loadTestRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void execute(UUID serviceId, LoadTestScenario scenario, Consumer<RawRequestResult> resultConsumer) {
        log.info("Starting virtual thread load test: serviceId={}, virtualUsers={}, duration={}s, runs={}",
                serviceId, scenario.getVirtualUsers(), scenario.getDurationSeconds(), scenario.getRuns());
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            for (int i = 0; i < scenario.getVirtualUsers(); i++) {
                final int workerId = i;
                scope.fork(() -> {
                    runWorker(workerId, scenario, resultConsumer);
                    return null;
                });
            }

            scope.join();
            scope.throwIfFailed();

            log.info("Load test execution completed for service {}", serviceId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Load test interrupted for service {}", serviceId, e);
            throw new RuntimeException("Load test interrupted", e);
        } catch (ExecutionException e) {
            log.error("Load test failed for service {}", serviceId, e);
            throw new RuntimeException("Load test failed", e);
        }
    }

    public CancellableTestHandle executeWithCancellation(UUID serviceId, LoadTestScenario scenario, Consumer<RawRequestResult> resultConsumer) {

        var scope = new StructuredTaskScope.ShutdownOnSuccess<Void>();

        Thread executorThread = Thread.startVirtualThread(() -> {
            try {
                for (int i = 0; i < scenario.getVirtualUsers(); i++) {
                    final int workerId = i;
                    scope.fork(() -> {
                        runWorker(workerId, scenario, resultConsumer);
                        return null;
                    });
                }

                scope.join();
                log.info("Load test completed for service {}", serviceId);
            } catch (InterruptedException e) {
                log.info("Load test cancelled for service {}", serviceId);
                Thread.currentThread().interrupt();
            }
        });

        return new CancellableTestHandle(scope, executorThread);
    }

    /**
     * Execute async using virtual thread.
     */
    public CompletableFuture<Void> executeAsync(UUID serviceId, LoadTestScenario scenario, Consumer<RawRequestResult> resultConsumer) {
        return CompletableFuture.runAsync(
                () -> execute(serviceId, scenario, resultConsumer),
                Executors.newVirtualThreadPerTaskExecutor()
        );
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
            log.error("Worker {} encountered error", workerId, e);
            throw new RuntimeException("Worker failed", e);
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

    /**
     * Handle for cancellable test execution.
     */
    public static class CancellableTestHandle {
        private final StructuredTaskScope<Void> scope;
        private final Thread executorThread;

        CancellableTestHandle(StructuredTaskScope<Void> scope, Thread executorThread) {
            this.scope = scope;
            this.executorThread = executorThread;
        }

        public void cancel() {
            scope.shutdown();
            executorThread.interrupt();
        }

        public void await() throws InterruptedException {
            executorThread.join();
        }

        public boolean isRunning() {
            return executorThread.isAlive();
        }
    }

}
