package com.auzienko.observability.corebackend.loadtester.model;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public record LoadTestMetrics(

        long totalRequests,
        long successfulRequests,
        long failedRequests,
        long avgResponseTimeMs,
        long p50ResponseTimeMs,
        long p95ResponseTimeMs,
        long p99ResponseTimeMs,
        long minResponseTimeMs,
        long maxResponseTimeMs,
        Map<String, AtomicLong> errorsByType,
        Map<String, StepMetrics> stepMetrics

) {

    public double getSuccessRate() {
        return totalRequests > 0 ? (successfulRequests * 100.0) / totalRequests : 0.0;
    }

}
