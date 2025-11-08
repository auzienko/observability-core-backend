package com.auzienko.observability.corebackend.domain.repository;

import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthCheckRepository {

    HealthCheckResult save(HealthCheckResult healthCheckResult);

    Optional<HealthCheckResult> findById(UUID id);

    List<HealthCheckResult> findAll();

    void deleteById(UUID id);

    List<HealthCheckResult> findHistoryByServiceIdSince(UUID serviceId, Instant startTime);

}
