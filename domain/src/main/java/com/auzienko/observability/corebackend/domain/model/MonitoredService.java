package com.auzienko.observability.corebackend.domain.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class MonitoredService {

    private UUID id;
    private String name;
    private String healthCheckScenario;
    private Integer pollingIntervalSeconds;
    private ServiceStatus status;
    private Instant lastCheckedAt;
    private Long avgResponseTimeMs;

}
