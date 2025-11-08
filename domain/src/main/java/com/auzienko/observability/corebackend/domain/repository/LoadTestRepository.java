package com.auzienko.observability.corebackend.domain.repository;

import com.auzienko.observability.corebackend.domain.model.LoadTestResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadTestRepository {

    LoadTestResult save(LoadTestResult loadTestResult);

    Optional<LoadTestResult> findById(UUID id);

    List<LoadTestResult> findAll();

    void deleteById(UUID id);

}
