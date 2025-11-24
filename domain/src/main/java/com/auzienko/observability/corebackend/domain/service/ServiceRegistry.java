package com.auzienko.observability.corebackend.domain.service;

import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;
import com.auzienko.observability.corebackend.domain.model.MonitoredService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRegistry {

    MonitoredService registerService(MonitoredService service);

    MonitoredService updateService(UUID id, MonitoredService service);

    void unregisterService(UUID id);

    Optional<MonitoredService> findServiceById(UUID id);

    List<MonitoredService> findAllServices();

    List<HealthCheckResult> getHealthHistoryForService(UUID serviceId, Duration duration);
}
