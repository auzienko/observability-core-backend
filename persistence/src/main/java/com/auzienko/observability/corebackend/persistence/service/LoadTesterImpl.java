package com.auzienko.observability.corebackend.persistence.service;

import com.auzienko.observability.corebackend.domain.model.HttpRequest;
import com.auzienko.observability.corebackend.domain.model.LoadTestResult;
import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;
import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.model.Step;
import com.auzienko.observability.corebackend.domain.repository.LoadTestRepository;
import com.auzienko.observability.corebackend.domain.repository.MonitoredServiceRepository;
import com.auzienko.observability.corebackend.domain.service.LoadTester;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
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
import java.util.stream.IntStream;

@Service
@Slf4j
public class LoadTesterImpl implements LoadTester {

    private final MonitoredServiceRepository monitoredServiceRepository;
    private final LoadTestRepository loadTestRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LoadTesterImpl(
            MonitoredServiceRepository monitoredServiceRepository,
            LoadTestRepository loadTestRepository,
            @Qualifier("loadTestRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.monitoredServiceRepository = monitoredServiceRepository;
        this.loadTestRepository = loadTestRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Async("loadTestExecutor")
    public CompletableFuture<LoadTestResult> startLoadTest(UUID serviceId, LoadTestScenario scenario) {
        log.info("Starting load test: serviceId={}, virtualUsers={}, duration={}s",
                serviceId, scenario.getVirtualUsers(), scenario.getDurationSeconds());

        MonitoredService service = findService(serviceId);
        LoadTestExecutionResult executionResult = executeLoadTest(scenario);
        LoadTestResult result = buildAndSaveResult(serviceId, scenario.getDurationSeconds(), executionResult);

        log.info("Load test completed: serviceId={}, totalRequests={}, successRate={}%, rps={}",
                serviceId, result.getTotalRequests(),
                calculateSuccessRate(result), result.getRequestsPerSecond());

        return CompletableFuture.completedFuture(result);
    }

    private MonitoredService findService(UUID serviceId) {
        return monitoredServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found: " + serviceId));
    }

    private LoadTestExecutionResult executeLoadTest(LoadTestScenario scenario) {
        try (ExecutorService executor = createExecutor(scenario.getVirtualUsers())) {
            List<Future<List<RequestResult>>> futures = submitWorkers(executor, scenario);
            List<RequestResult> allResults = collectResults(futures);
            return aggregateResults(allResults);
        }
    }

    private ExecutorService createExecutor(int virtualUsers) {
        return Executors.newFixedThreadPool(virtualUsers);
    }

    private List<Future<List<RequestResult>>> submitWorkers(ExecutorService executor, LoadTestScenario scenario) {
        long testEndTime = System.currentTimeMillis() + (scenario.getDurationSeconds() * 1000L);

        return IntStream.range(0, scenario.getVirtualUsers())
                .mapToObj(workerId -> executor.submit(() -> runWorker(scenario, testEndTime, workerId)))
                .toList();
    }

    private List<RequestResult> runWorker(LoadTestScenario scenario, long testEndTime, int workerId) {
        List<RequestResult> localResults = new ArrayList<>();
        UserContext context = new UserContext();

        while (System.currentTimeMillis() < testEndTime && !Thread.currentThread().isInterrupted()) {
            for (Step step : scenario.getSteps()) {
                RequestResult result = executeStep(step, context);
                localResults.add(result);
            }
        }

        log.debug("Worker {} completed with {} requests", workerId, localResults.size());
        return localResults;
    }

    private RequestResult executeStep(Step step, UserContext context) {
        HttpEntity<String> requestEntity = prepareRequest(step.getRequest(), context);
        String url = resolveVariables(step.getRequest().getUrl(), context);
        HttpMethod method = HttpMethod.valueOf(step.getRequest().getMethod().toUpperCase());

        long startTime = System.nanoTime();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, requestEntity, String.class);
            long responseTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            extractVariables(response.getBody(), step.getExtract(), context);

            return RequestResult.success(responseTimeMs);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            return RequestResult.failure("CONNECTION_ERROR");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            return RequestResult.failure("HTTP_4XX_" + e.getStatusCode().value());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            return RequestResult.failure("HTTP_5XX_" + e.getStatusCode().value());
        } catch (Exception e) {
            log.debug("Unexpected error during request", e);
            return RequestResult.failure("UNKNOWN_ERROR");
        }
    }

    private List<RequestResult> collectResults(List<Future<List<RequestResult>>> futures) {
        List<RequestResult> allResults = new ArrayList<>();

        for (Future<List<RequestResult>> future : futures) {
            try {
                List<RequestResult> workerResults = future.get();
                allResults.addAll(workerResults);
            } catch (ExecutionException e) {
                log.error("Worker task failed", e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Result collection interrupted", e);
                break;
            }
        }

        return allResults;
    }

    private LoadTestExecutionResult aggregateResults(List<RequestResult> allResults) {
        List<Long> successfulTimes = new ArrayList<>();
        long total = 0;
        long successful = 0;
        long failed = 0;
        Map<String, Long> errors = new HashMap<>();

        for (RequestResult result : allResults) {
            total++;
            if (result.success()) {
                successful++;
                successfulTimes.add(result.responseTimeMs());
            } else {
                failed++;
                errors.merge(result.errorType(), 1L, Long::sum);
            }
        }

        successfulTimes.sort(Long::compareTo);
        return new LoadTestExecutionResult(successfulTimes, total, successful, failed, errors);
    }

    private LoadTestResult buildAndSaveResult(UUID serviceId, int durationSeconds,
                                              LoadTestExecutionResult executionResult) {
        MetricsCalculator calculator = new MetricsCalculator(executionResult.successfulResponseTimes());

        LoadTestResult result = new LoadTestResult();
        result.setServiceId(serviceId);
        result.setExecutedAt(Instant.now());
        result.setTotalRequests(executionResult.totalRequests());
        result.setSuccessfulRequests(executionResult.successfulRequests());
        result.setAvgResponseTimeMs(calculator.average());
        result.setP95ResponseTimeMs(calculator.percentile(0.95));
        result.setP99ResponseTimeMs(calculator.percentile(0.99));
        result.setRequestsPerSecond(executionResult.totalRequests() / (double) durationSeconds);

        logErrorsIfPresent(executionResult.errorsByType());
        loadTestRepository.save(result);

        return result;
    }

    private void logErrorsIfPresent(Map<String, Long> errorsByType) {
        if (!errorsByType.isEmpty()) {
            log.warn("Load test errors: {}", errorsByType);
        }
    }

    private double calculateSuccessRate(LoadTestResult result) {
        if (result.getTotalRequests() == 0) return 0.0;
        return (result.getSuccessfulRequests() * 100.0) / result.getTotalRequests();
    }

    // ============= Internal Classes =============

    @Getter
    private static class UserContext {

        private final Map<String, Object> variables = new HashMap<>();

        public void setVariable(String name, Object value) {
            this.variables.put(name, value);
        }

    }

    private record RequestResult(long responseTimeMs, boolean success, String errorType) {
        static RequestResult success(long responseTimeMs) {
            return new RequestResult(responseTimeMs, true, null);
        }

        static RequestResult failure(String errorType) {
            return new RequestResult(-1, false, errorType);
        }
    }

    private record LoadTestExecutionResult(
            List<Long> successfulResponseTimes,
            long totalRequests,
            long successfulRequests,
            long failedRequests,
            Map<String, Long> errorsByType
    ) {
    }

    private record MetricsCalculator(List<Long> sortedTimes) {

        long average() {
            if (sortedTimes.isEmpty()) return 0;
            return (long) sortedTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
        }

        long percentile(double percentile) {
            if (sortedTimes.isEmpty()) return 0;
            if (sortedTimes.size() == 1) return sortedTimes.get(0);

            int index = (int) Math.ceil(sortedTimes.size() * percentile) - 1;
            index = Math.max(0, Math.min(index, sortedTimes.size() - 1));

            return sortedTimes.get(index);
        }

        long min() {
            return sortedTimes.isEmpty() ? 0 : sortedTimes.get(0);
        }

        long max() {
            return sortedTimes.isEmpty() ? 0 : sortedTimes.get(sortedTimes.size() - 1);
        }
    }

    private HttpEntity<String> prepareRequest(HttpRequest request, UserContext context) {
        HttpHeaders headers = new HttpHeaders();
        if (request.getHeaders() != null) {
            request.getHeaders().forEach((key, value) -> {
                headers.set(key, resolveVariables(value, context));
            });
        }

        String body = "";
        if (request.getBody() != null) {
            // A more advanced implementation would handle raw JSON strings too.
            try {
                Map<String, Object> resolvedBody = new HashMap<>();
                request.getBody().forEach((key, value) -> {
                    resolvedBody.put(key, (value instanceof String) ? resolveVariables((String) value, context) : value);
                });
                body = objectMapper.writeValueAsString(resolvedBody);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize request body", e);
                // Handle error appropriately
            }
        }

        return new HttpEntity<>(body, headers);
    }

    private String resolveVariables(String input, UserContext context) {
        if (input == null || !input.contains("${")) {
            return input;
        }

        String result = input;

        // Built-in functions
        if (result.contains("${randomUUID}")) {
            result = result.replace("${randomUUID}", UUID.randomUUID().toString());
        }

        // Context variables
        for (Map.Entry<String, Object> entry : context.getVariables().entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        return result;
    }

    private void extractVariables(String responseBody, Map<String, String> extractConfig, UserContext context) {
        if (extractConfig == null || responseBody == null || responseBody.isEmpty()) {
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
                    log.trace("Extracted '{}' with value '{}' using path '{}'", variableName, extractedValue, jsonPath);
                } catch (Exception e) {
                    log.warn("Failed to extract variable '{}' with JSONPath '{}': {}", variableName, jsonPath, e.getMessage());
                }
            }
        }
    }

}
