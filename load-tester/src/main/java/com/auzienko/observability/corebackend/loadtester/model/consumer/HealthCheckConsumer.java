package com.auzienko.observability.corebackend.loadtester.model.consumer;

import com.auzienko.observability.corebackend.loadtester.model.HealthCheckTestResult;
import com.auzienko.observability.corebackend.loadtester.model.RawRequestResult;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Simple consumer for health checks.
 * Only cares if all requests succeeded.
 */
public class HealthCheckConsumer implements Consumer<RawRequestResult> {

    @Getter
    private final AtomicLong totalRequests = new AtomicLong(0);

    @Getter
    private final AtomicLong failedRequests = new AtomicLong(0);

    private volatile RawRequestResult firstFailure = null;

    @Override
    public void accept(RawRequestResult result) {
        totalRequests.incrementAndGet();

        if (!result.isSuccess()) {
            failedRequests.incrementAndGet();
            // Store first failure for diagnostics
            if (firstFailure == null) {
                firstFailure = result;
            }
        }
    }

    public boolean isHealthy() {
        return failedRequests.get() == 0;
    }

    public RawRequestResult getFirstFailure() {
        return firstFailure;
    }

    public HealthCheckTestResult getResult() {
        return new HealthCheckTestResult(
                totalRequests.get(),
                failedRequests.get() == 0,
                firstFailure
        );
    }

}
