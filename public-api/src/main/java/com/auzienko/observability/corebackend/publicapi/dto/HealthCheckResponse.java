package com.auzienko.observability.corebackend.publicapi.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class HealthCheckResponse {

    private UUID id;
    private UUID serviceId;
    private Instant timestamp;
    private String status;
    private String errorMessage;

}
