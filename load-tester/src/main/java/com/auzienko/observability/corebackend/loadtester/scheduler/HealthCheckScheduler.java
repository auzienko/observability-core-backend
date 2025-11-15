package com.auzienko.observability.corebackend.loadtester.scheduler;

import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.repository.MonitoredServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckScheduler {

    private final MonitoredServiceRepository monitoredServiceRepository;
    private final HealthCheckExecutor executor;

    @Scheduled(fixedRateString = "${app.monitoring.scheduler.fixed-rate-ms}")
    public void performHealthChecks() {
        log.info("Starting scheduled health checks...");
        List<MonitoredService> services = monitoredServiceRepository.findAll();

        services.parallelStream().forEach(executor::checkService);
        log.info("Finished scheduled health checks for {} services.", services.size());
    }

}
