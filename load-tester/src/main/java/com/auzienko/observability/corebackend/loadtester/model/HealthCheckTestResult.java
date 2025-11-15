package com.auzienko.observability.corebackend.loadtester.model;

public record HealthCheckTestResult(

        long totalRequests,
        boolean healthy,
        RawRequestResult firstFailure

) {
}
