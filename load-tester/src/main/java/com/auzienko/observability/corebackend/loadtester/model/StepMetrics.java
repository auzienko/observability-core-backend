package com.auzienko.observability.corebackend.loadtester.model;

public record StepMetrics(
        long requestCount,
        long avgResponseTimeMs,
        long p95ResponseTimeMs,
        long p99ResponseTimeMs
) {
}