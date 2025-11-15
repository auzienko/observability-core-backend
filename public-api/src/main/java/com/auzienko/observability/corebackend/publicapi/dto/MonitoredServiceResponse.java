package com.auzienko.observability.corebackend.publicapi.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class MonitoredServiceResponse {

    private UUID id;
    private String name;
    private String status;
    private Instant lastCheckedAt;
    private String healthCheckScenario;

}
