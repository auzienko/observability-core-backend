package com.auzienko.observability.corebackend.loadtester.service;

import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;
import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;
import com.auzienko.observability.corebackend.domain.model.ServiceStatus;
import com.auzienko.observability.corebackend.domain.repository.HealthCheckRepository;
import com.auzienko.observability.corebackend.loadtester.model.HealthCheckTestResult;
import com.auzienko.observability.corebackend.loadtester.model.consumer.CompositeConsumer;
import com.auzienko.observability.corebackend.loadtester.model.consumer.DebugConsumer;
import com.auzienko.observability.corebackend.loadtester.model.consumer.HealthCheckConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class HealthCheckService {

    private final VirtualThreadLoadTestExecutor executor;
    private final HealthCheckRepository healthCheckRepository;

    /**
     * Execute health check and save result.
     */
    @Async
    public CompletableFuture<HealthCheckTestResult> execute(UUID serviceId, LoadTestScenario scenario) {
        log.info("Starting health check for service {}", serviceId);

        return CompletableFuture.supplyAsync(() -> {
            LoadTestScenario healthCheckScenario = toHealthCheckScenario(scenario);

            HealthCheckConsumer consumer = new HealthCheckConsumer();

            executor.execute(serviceId, healthCheckScenario, consumer);

            HealthCheckTestResult consumerResult = consumer.getResult();

            HealthCheckResult domainResult = toDomainModel(serviceId, consumerResult);

            healthCheckRepository.save(domainResult);

            log.info("Health check completed: serviceId={}, healthy={}",
                    serviceId, consumerResult.healthy());

            return consumerResult;
        });
    }

    /**
     * Execute health check with detailed logging.
     */
    @Async
    public CompletableFuture<HealthCheckTestResult> executeWithLogging(UUID serviceId, LoadTestScenario scenario) {
        log.info("Starting health check with logging for service {}", serviceId);

        return CompletableFuture.supplyAsync(() -> {
            LoadTestScenario healthCheckScenario = toHealthCheckScenario(scenario);

            HealthCheckConsumer healthConsumer = new HealthCheckConsumer();
            DebugConsumer debugConsumer = new DebugConsumer(false, true, true); // Only log failures
            CompositeConsumer composite = new CompositeConsumer(healthConsumer, debugConsumer);

            executor.execute(serviceId, healthCheckScenario, composite);

            HealthCheckTestResult result = healthConsumer.getResult();

            if (!result.healthy()) {
                log.error("Health check failed for service {}: {}",
                        serviceId, result.firstFailure().getErrorMessage());
            }

            return result;
        });
    }

    private LoadTestScenario toHealthCheckScenario(LoadTestScenario scenario) {
        LoadTestScenario healthCheck = new LoadTestScenario();
        healthCheck.setSteps(scenario.getSteps());
        healthCheck.setRuns(1);
        healthCheck.setVirtualUsers(1);
        healthCheck.setDurationSeconds(null);
        return healthCheck;
    }

    private HealthCheckResult toDomainModel(
            UUID serviceId, HealthCheckTestResult consumerResult) {

        HealthCheckResult result = new HealthCheckResult();

        result.setServiceId(serviceId);
        result.setStatus(consumerResult.healthy()
                ? ServiceStatus.UP
                : ServiceStatus.DOWN
        );

        if (!consumerResult.healthy() && consumerResult.firstFailure() != null) {
            result.setErrorMessage(consumerResult.firstFailure().getErrorMessage());
        }

        return result;
    }

}
