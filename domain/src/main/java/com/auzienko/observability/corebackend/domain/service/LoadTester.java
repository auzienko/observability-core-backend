package com.auzienko.observability.corebackend.domain.service;

import com.auzienko.observability.corebackend.domain.model.LoadTestResult;
import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LoadTester {

    CompletableFuture<LoadTestResult> startLoadTest(UUID serviceId, LoadTestScenario scenario);

}
