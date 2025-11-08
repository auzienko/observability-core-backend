package com.auzienko.observability.corebackend.persistence.repository;

import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.repository.MonitoredServiceRepository;
import com.auzienko.observability.corebackend.persistence.mapper.MonitoredServiceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MonitoredServiceRepositoryImpl implements MonitoredServiceRepository {

    private final MonitoredServiceJpaRepository jpaRepository;
    private final MonitoredServiceMapper mapper;

    @Override
    public MonitoredService save(MonitoredService service) {
        var entity = mapper.toEntity(service);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MonitoredService> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<MonitoredService> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

}
