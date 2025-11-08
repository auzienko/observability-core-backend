package com.auzienko.observability.corebackend.domain.repository;

import com.auzienko.observability.corebackend.domain.model.MonitoredService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitoredServiceRepository {

    MonitoredService save(MonitoredService service);

    Optional<MonitoredService> findById(UUID id);

    List<MonitoredService> findAll();

    void deleteById(UUID id);

}
