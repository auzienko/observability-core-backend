package com.auzienko.observability.corebackend.publicapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MonitoredServiceRequest {

    @NotBlank(message = "Service name cannot be blank")
    private String name;

    @NotBlank(message = "Health check scenario cannot be blank")
    private String healthCheckScenario;

    @NotNull(message = "Polling interval must be provided")
    @Positive(message = "Polling interval must be a positive number")
    private Integer pollingIntervalSeconds;

}
