package com.auzienko.observability.corebackend.domain.service;

import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;
import com.auzienko.observability.corebackend.domain.model.MonitoredService;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface DashboardService {

    List<MonitoredService> getCurrentServicesStatus();

    List<HealthCheckResult> getHealthHistoryForService(UUID serviceId, Duration duration);

}
