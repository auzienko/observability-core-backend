package com.auzienko.observability.corebackend.persistence.service;

import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;
import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.repository.HealthCheckRepository;
import com.auzienko.observability.corebackend.domain.repository.MonitoredServiceRepository;
import com.auzienko.observability.corebackend.domain.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final MonitoredServiceRepository monitoredServiceRepository;
    private final HealthCheckRepository healthCheckRepository;

    @Override
    public List<MonitoredService> getCurrentServicesStatus() {
        return monitoredServiceRepository.findAll();
    }

    @Override
    public List<HealthCheckResult> getHealthHistoryForService(UUID serviceId, Duration duration) {
        Instant startTime = Instant.now().minus(duration);
        return healthCheckRepository.findHistoryByServiceIdSince(serviceId, startTime);
    }

}
