package com.auzienko.observability.corebackend.persistence.repository;

import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;
import com.auzienko.observability.corebackend.domain.repository.HealthCheckRepository;
import com.auzienko.observability.corebackend.persistence.mapper.HealthCheckResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class HealthCheckRepositoryImpl implements HealthCheckRepository {

    private final HealthCheckResultJpaRepository jpaRepository;
    private final HealthCheckResultMapper mapper;

    @Override
    public HealthCheckResult save(HealthCheckResult healthCheckResult) {
        var entity = mapper.toEntity(healthCheckResult);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<HealthCheckResult> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<HealthCheckResult> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<HealthCheckResult> findHistoryByServiceIdSince(UUID serviceId, Instant startTime) {
        return jpaRepository.findHistoryByServiceIdSince(serviceId, startTime).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

}
