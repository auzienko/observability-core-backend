package com.auzienko.observability.corebackend.domain.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class LoadTestResult {

    private UUID id;
    private UUID serviceId;
    private Instant executedAt;
    private long totalRequests;
    private long successfulRequests;
    private long avgResponseTimeMs;
    private long p95ResponseTimeMs;
    private long p99ResponseTimeMs;
    private double requestsPerSecond;

}
