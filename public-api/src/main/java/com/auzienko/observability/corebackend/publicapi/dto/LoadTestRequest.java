package com.auzienko.observability.corebackend.publicapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoadTestRequest {

    @NotNull
    @Min(value = 1, message = "Must have at least 1 virtual user")
    @Max(value = 100, message = "Cannot exceed 100 virtual users")
    private Integer virtualUsers;

    @NotNull
    @Min(value = 10, message = "Duration must be at least 10 seconds")
    @Max(value = 300, message = "Duration cannot exceed 300 seconds")
    private Integer durationSeconds;

}
