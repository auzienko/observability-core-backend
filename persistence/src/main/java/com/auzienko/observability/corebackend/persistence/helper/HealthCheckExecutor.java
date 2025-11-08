package com.auzienko.observability.corebackend.persistence.helper;

import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;
import com.auzienko.observability.corebackend.domain.model.LoadTestResult;
import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;
import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.model.ServiceStatus;
import com.auzienko.observability.corebackend.domain.repository.HealthCheckRepository;
import com.auzienko.observability.corebackend.domain.repository.MonitoredServiceRepository;
import com.auzienko.observability.corebackend.domain.service.LoadTester;
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
    private final HealthCheckRepository healthCheckRepository;
    private final LoadTester loadTester;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkService(MonitoredService service) {
        long startTime = System.currentTimeMillis();
        try {
            LoadTestScenario scenario = objectMapper.readValue(service.getHealthCheckScenario(), LoadTestScenario.class);
            CompletableFuture<LoadTestResult> futureResult = loadTester.startLoadTest(service.getId(), scenario);
            LoadTestResult result = futureResult.get(15, TimeUnit.SECONDS);

            updateServiceStatusFromResult(service, result);

        } catch (Exception e) {
            log.warn("Health check failed for service '{}' ({}): {}", service.getName(), service.getId(), e.getMessage());
            updateServiceStatusFromError(service);
        }
    }

    private void updateServiceStatusFromResult(MonitoredService service, LoadTestResult result) {
        ServiceStatus newStatus = (result.getSuccessfulRequests() > 0) ? ServiceStatus.UP : ServiceStatus.DOWN;

        HealthCheckResult healthResult = new HealthCheckResult();
        healthResult.setServiceId(service.getId());
        healthResult.setStatus(newStatus);
        healthResult.setResponseTimeMs(result.getAvgResponseTimeMs());

        service.setStatus(newStatus);
        service.setLastCheckedAt(Instant.now());
        service.setAvgResponseTimeMs(result.getAvgResponseTimeMs());

        try {
            healthCheckRepository.save(healthResult);
            monitoredServiceRepository.save(service);
            log.debug("Updated status for service '{}' to {} with response time {}ms.",
                    service.getName(), newStatus, result.getAvgResponseTimeMs());
        } catch (Exception e) {
            log.error("Failed to save health check result for service '{}'", service.getName(), e);
        }
    }

    private void updateServiceStatusFromError(MonitoredService service) {
        ServiceStatus newStatus = ServiceStatus.DOWN;

        HealthCheckResult healthResult = new HealthCheckResult();
        healthResult.setServiceId(service.getId());
        healthResult.setStatus(newStatus);
        healthResult.setResponseTimeMs(null);
        healthResult.setHttpStatusCode(null);

        service.setStatus(newStatus);
        service.setLastCheckedAt(Instant.now());
        service.setAvgResponseTimeMs(null);

        try {
            healthCheckRepository.save(healthResult);
            monitoredServiceRepository.save(service);
            log.warn("Updated status for service '{}' to DOWN due to a check execution error.", service.getName());
        } catch (Exception e) {
            log.error("Failed to save FAILED health check result for service '{}'", service.getName(), e);
        }
    }

}
