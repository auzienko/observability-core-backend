package com.auzienko.observability.corebackend.loadtester.model.consumer;

import com.auzienko.observability.corebackend.loadtester.model.LoadTestMetrics;
import com.auzienko.observability.corebackend.loadtester.model.RawRequestResult;
import com.auzienko.observability.corebackend.loadtester.model.StepMetrics;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Aggregating consumer for load test metrics.
 * Calculates statistics like p95, p99, etc.
 */
public class MetricsConsumer implements Consumer<RawRequestResult> {

    private final List<Long> successfulResponseTimes = Collections.synchronizedList(new ArrayList<>());

    @Getter
    private final AtomicLong totalRequests = new AtomicLong(0);

    @Getter
    private final AtomicLong successfulRequests = new AtomicLong(0);

    @Getter
    private final AtomicLong failedRequests = new AtomicLong(0);

    private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestsByStep = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> responseTimesByStep = new ConcurrentHashMap<>();

    @Override
    public void accept(RawRequestResult result) {
        totalRequests.incrementAndGet();

        // Track by step
        String stepKey = "step_" + result.getStepIndex() + "_" +
                (result.getStepName() != null ? result.getStepName() : "unnamed");
        requestsByStep.computeIfAbsent(stepKey, k -> new AtomicLong(0)).incrementAndGet();

        if (result.isSuccess()) {
            successfulRequests.incrementAndGet();
            long durationMs = result.getDurationMs();
            successfulResponseTimes.add(durationMs);

            // Track response times by step
            responseTimesByStep
                    .computeIfAbsent(stepKey, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(durationMs);

        } else {
            failedRequests.incrementAndGet();
            String errorType = result.getErrorType() != null ? result.getErrorType() : "UNKNOWN";
            errorsByType.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    public LoadTestMetrics getMetrics() {
        List<Long> sortedTimes = new ArrayList<>(successfulResponseTimes);
        sortedTimes.sort(Long::compareTo);

        Map<String, StepMetrics> stepMetrics = new HashMap<>();
        responseTimesByStep.forEach((stepKey, times) -> {
            List<Long> sortedStepTimes = new ArrayList<>(times);
            sortedStepTimes.sort(Long::compareTo);
            stepMetrics.put(stepKey, calculateStepMetrics(sortedStepTimes));
        });

        return new LoadTestMetrics(
                totalRequests.get(),
                successfulRequests.get(),
                failedRequests.get(),
                calculateAverage(sortedTimes),
                calculatePercentile(sortedTimes, 0.50),
                calculatePercentile(sortedTimes, 0.95),
                calculatePercentile(sortedTimes, 0.99),
                sortedTimes.isEmpty() ? 0 : sortedTimes.get(0),
                sortedTimes.isEmpty() ? 0 : sortedTimes.get(sortedTimes.size() - 1),
                new HashMap<>(errorsByType),
                stepMetrics
        );
    }

    private StepMetrics calculateStepMetrics(List<Long> sortedTimes) {
        return new StepMetrics(
                sortedTimes.size(),
                calculateAverage(sortedTimes),
                calculatePercentile(sortedTimes, 0.95),
                calculatePercentile(sortedTimes, 0.99)
        );
    }

    private long calculateAverage(List<Long> times) {
        if (times.isEmpty()) return 0;
        return (long) times.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private long calculatePercentile(List<Long> sortedTimes, double percentile) {
        if (sortedTimes.isEmpty()) return 0;
        if (sortedTimes.size() == 1) return sortedTimes.get(0);
        int index = (int) Math.ceil(sortedTimes.size() * percentile) - 1;
        index = Math.max(0, Math.min(index, sortedTimes.size() - 1));
        return sortedTimes.get(index);
    }

}
