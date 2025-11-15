package com.auzienko.observability.corebackend.loadtester.scheduler;

import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;
import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.model.ServiceStatus;
import com.auzienko.observability.corebackend.domain.repository.MonitoredServiceRepository;
import com.auzienko.observability.corebackend.loadtester.model.HealthCheckTestResult;
import com.auzienko.observability.corebackend.loadtester.service.HealthCheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthCheckExecutor {

    private final MonitoredServiceRepository monitoredServiceRepository;
    private final HealthCheckService healthCheckService;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkService(MonitoredService service) {
        try {
            LoadTestScenario scenario = objectMapper.readValue(service.getHealthCheckScenario(), LoadTestScenario.class);
            CompletableFuture<HealthCheckTestResult> futureResult = healthCheckService.execute(service.getId(), scenario);
            HealthCheckTestResult result = futureResult.get(15, TimeUnit.SECONDS);

            updateServiceStatusFromResult(service, result);

        } catch (Exception e) {
            log.warn("Health check failed for service '{}' ({}): {}", service.getName(), service.getId(), e.getMessage());
            updateServiceStatusFromError(service);
        }
    }

    private void updateServiceStatusFromResult(MonitoredService service, HealthCheckTestResult result) {
        ServiceStatus newStatus = result.healthy() ? ServiceStatus.UP : ServiceStatus.DOWN;

        service.setStatus(newStatus);
        service.setLastCheckedAt(Instant.now());

        try {
            monitoredServiceRepository.save(service);
            log.debug("Updated status for service '{}' to {}", service.getName(), newStatus);
        } catch (Exception e) {
            log.error("Failed to save health check result for service '{}'", service.getName(), e);
        }
    }

    private void updateServiceStatusFromError(MonitoredService service) {
        ServiceStatus newStatus = ServiceStatus.DOWN;

        service.setStatus(newStatus);
        service.setLastCheckedAt(Instant.now());

        try {
            monitoredServiceRepository.save(service);
            log.warn("Updated status for service '{}' to DOWN due to a check execution error.", service.getName());
        } catch (Exception e) {
            log.error("Failed to save FAILED health check result for service '{}'", service.getName(), e);
        }
    }

}
