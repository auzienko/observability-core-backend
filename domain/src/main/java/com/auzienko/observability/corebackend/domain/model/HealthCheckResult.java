package com.auzienko.observability.corebackend.domain.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class HealthCheckResult {

    private UUID id;
    private UUID serviceId;
    private Instant timestamp;
    private ServiceStatus status;
    private String errorMessage;

}
