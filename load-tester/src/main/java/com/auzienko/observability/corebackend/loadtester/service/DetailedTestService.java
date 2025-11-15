package com.auzienko.observability.corebackend.loadtester.service;

import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;
import com.auzienko.observability.corebackend.loadtester.model.LoadTestMetrics;
import com.auzienko.observability.corebackend.loadtester.model.RawRequestResult;
import com.auzienko.observability.corebackend.loadtester.model.consumer.CompositeConsumer;
import com.auzienko.observability.corebackend.loadtester.model.consumer.MetricsConsumer;
import com.auzienko.observability.corebackend.loadtester.model.consumer.StorageConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class DetailedTestService {

    private final LoadTesterExecutor executor;

    /**
     * Execute test and store all results (including successful ones).
     * Use for detailed analysis or debugging.
     */
    public CompletableFuture<DetailedTestResult> executeDetailed(
            UUID serviceId,
            LoadTestScenario scenario,
            int maxResultsToStore) {

        log.info("Starting detailed test for service {}", serviceId);

        return CompletableFuture.supplyAsync(() -> {
            MetricsConsumer metricsConsumer = new MetricsConsumer();
            StorageConsumer storageConsumer = new StorageConsumer(false, maxResultsToStore);
            CompositeConsumer composite = new CompositeConsumer(metricsConsumer, storageConsumer);

            executor.execute(serviceId, scenario, composite);

            return new DetailedTestResult(
                    metricsConsumer.getMetrics(),
                    storageConsumer.getResults()
            );
        });
    }

    /**
     * Execute test but only store failures.
     * More memory efficient while keeping diagnostic data.
     */
    public CompletableFuture<DetailedTestResult> executeWithFailureTracking(
            UUID serviceId,
            LoadTestScenario scenario) {

        log.info("Starting test with failure tracking for service {}", serviceId);

        return CompletableFuture.supplyAsync(() -> {
            MetricsConsumer metricsConsumer = new MetricsConsumer();
            StorageConsumer storageConsumer = new StorageConsumer(true, 1000); // Only failures
            CompositeConsumer composite = new CompositeConsumer(metricsConsumer, storageConsumer);

            executor.execute(serviceId, scenario, composite);

            return new DetailedTestResult(
                    metricsConsumer.getMetrics(),
                    storageConsumer.getResults()
            );
        });
    }

    public record DetailedTestResult(
            LoadTestMetrics metrics,
            List<RawRequestResult> rawResults
    ) {
    }

}
