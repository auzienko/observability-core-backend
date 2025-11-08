package com.auzienko.observability.corebackend.persistence.repository;

import com.auzienko.observability.corebackend.domain.model.LoadTestResult;
import com.auzienko.observability.corebackend.domain.repository.LoadTestRepository;
import com.auzienko.observability.corebackend.persistence.mapper.LoadTestResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LoadTestRepositoryImpl implements LoadTestRepository {

    private final LoadTestResultJpaRepository jpaRepository;
    private final LoadTestResultMapper mapper;

    @Override
    public LoadTestResult save(LoadTestResult loadTestResult) {
        var entity = mapper.toEntity(loadTestResult);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<LoadTestResult> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<LoadTestResult> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

}
