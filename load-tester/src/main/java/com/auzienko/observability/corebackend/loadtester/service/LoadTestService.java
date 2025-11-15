package com.auzienko.observability.corebackend.loadtester.service;

import com.auzienko.observability.corebackend.domain.model.LoadTestResult;
import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;
import com.auzienko.observability.corebackend.domain.repository.LoadTestRepository;
import com.auzienko.observability.corebackend.loadtester.model.LoadTestMetrics;
import com.auzienko.observability.corebackend.loadtester.model.consumer.CompositeConsumer;
import com.auzienko.observability.corebackend.loadtester.model.consumer.DebugConsumer;
import com.auzienko.observability.corebackend.loadtester.model.consumer.MetricsConsumer;
import com.auzienko.observability.corebackend.loadtester.model.consumer.ProgressConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * High-level service for load testing.
 * Orchestrates executor + consumers + persistence.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoadTestService {

    private final LoadTesterExecutor executor;
    private final LoadTestRepository loadTestRepository;

    /**
     * Execute load test and save aggregated results to DB.
     */
    @Async
    public CompletableFuture<LoadTestResult> executeAndSave(UUID serviceId, LoadTestScenario scenario) {
        log.info("Starting load test for service {}", serviceId);

        return CompletableFuture.supplyAsync(() -> {
            Instant startTime = Instant.now();

            MetricsConsumer metricsConsumer = new MetricsConsumer();

            executor.execute(serviceId, scenario, metricsConsumer);

            long durationMs = System.currentTimeMillis() - startTime.toEpochMilli();

            LoadTestMetrics metrics = metricsConsumer.getMetrics();

            LoadTestResult result = toLoadTestResult(serviceId, startTime, durationMs, metrics);
            LoadTestResult saved = loadTestRepository.save(result);

            log.info("Load test completed: serviceId={}, totalRequests={}, successRate={}%",
                    serviceId, result.getTotalRequests(), metrics.getSuccessRate());

            return saved;
        });
    }

    /**
     * Execute load test without saving (for preview/testing).
     */
    @Async
    public CompletableFuture<LoadTestMetrics> executePreview(UUID serviceId, LoadTestScenario scenario) {
        log.info("Starting preview load test for service {}", serviceId);

        return CompletableFuture.supplyAsync(() -> {
            MetricsConsumer metricsConsumer = new MetricsConsumer();
            executor.execute(serviceId, scenario, metricsConsumer);
            return metricsConsumer.getMetrics();
        });
    }

    /**
     * Execute load test with real-time progress updates.
     */
    @Async
    public CompletableFuture<LoadTestResult> executeWithProgress(
            UUID serviceId,
            LoadTestScenario scenario,
            Consumer<Double> progressCallback) {

        log.info("Starting load test with progress tracking for service {}", serviceId);

        return CompletableFuture.supplyAsync(() -> {
            Instant startTime = Instant.now();

            long expectedTotal = calculateExpectedTotal(scenario);

            // Composite consumer: metrics + progress
            MetricsConsumer metricsConsumer = new MetricsConsumer();
            ProgressConsumer progressConsumer = new ProgressConsumer(
                    expectedTotal,
                    progressCallback,
                    500 // Update every 500ms
            );

            CompositeConsumer composite = new CompositeConsumer(metricsConsumer, progressConsumer);

            executor.execute(serviceId, scenario, composite);

            long durationMs = System.currentTimeMillis() - startTime.toEpochMilli();
            LoadTestMetrics metrics = metricsConsumer.getMetrics();
            LoadTestResult result = toLoadTestResult(serviceId, startTime, durationMs, metrics);

            return loadTestRepository.save(result);
        });
    }

    /**
     * Execute load test in debug mode (logs all requests).
     */
    @Async
    public CompletableFuture<LoadTestMetrics> executeDebug(
            UUID serviceId,
            LoadTestScenario scenario,
            boolean logSuccessful,
            boolean logBodies) {

        log.info("Starting debug load test for service {}", serviceId);

        return CompletableFuture.supplyAsync(() -> {
            MetricsConsumer metricsConsumer = new MetricsConsumer();
            DebugConsumer debugConsumer = new DebugConsumer(logSuccessful, logBodies, logBodies);
            CompositeConsumer composite = new CompositeConsumer(metricsConsumer, debugConsumer);

            executor.execute(serviceId, scenario, composite);
            return metricsConsumer.getMetrics();
        });
    }

    private LoadTestResult toLoadTestResult(UUID serviceId, Instant startTime,
                                            long durationMs, LoadTestMetrics metrics) {
        LoadTestResult result = new LoadTestResult();
        result.setServiceId(serviceId);
        result.setExecutedAt(startTime);
        result.setTotalRequests(metrics.totalRequests());
        result.setSuccessfulRequests(metrics.successfulRequests());
        result.setAvgResponseTimeMs(metrics.avgResponseTimeMs());
        result.setMedianResponseTimeMs(metrics.p50ResponseTimeMs());
        result.setP95ResponseTimeMs(metrics.p95ResponseTimeMs());
        result.setP99ResponseTimeMs(metrics.p99ResponseTimeMs());
        result.setMinResponseTimeMs(metrics.minResponseTimeMs());
        result.setMaxResponseTimeMs(metrics.maxResponseTimeMs());

        double actualDurationSec = durationMs / 1000.0;
        result.setRequestsPerSecond(
                actualDurationSec > 0 ? metrics.totalRequests() / actualDurationSec : 0.0
        );

        return result;
    }

    private long calculateExpectedTotal(LoadTestScenario scenario) {
        int stepsPerIteration = scenario.getSteps().size();

        if (scenario.getRuns() != null) {
            return (long) scenario.getRuns() * scenario.getVirtualUsers() * stepsPerIteration;
        } else {
            // Estimate for duration-based (rough)
            return (long) scenario.getDurationSeconds() * scenario.getVirtualUsers() * 10;
        }
    }

}
