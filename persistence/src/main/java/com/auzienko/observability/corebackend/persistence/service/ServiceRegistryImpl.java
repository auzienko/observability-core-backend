package com.auzienko.observability.corebackend.persistence.service;

import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.repository.MonitoredServiceRepository;
import com.auzienko.observability.corebackend.domain.service.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceRegistryImpl implements ServiceRegistry {

    private final MonitoredServiceRepository monitoredServiceRepository;

    @Override
    public MonitoredService registerService(MonitoredService service) {
        return monitoredServiceRepository.save(service);
    }

    @Override
    public MonitoredService updateService(UUID id, MonitoredService serviceUpdate) {
        MonitoredService existingService = findServiceById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id)); // TODO Replace with a custom exception later

        existingService.setName(serviceUpdate.getName());
        existingService.setHealthCheckScenario(serviceUpdate.getHealthCheckScenario());
        existingService.setPollingIntervalSeconds(serviceUpdate.getPollingIntervalSeconds());

        return monitoredServiceRepository.save(existingService);
    }

    @Override
    public void unregisterService(UUID id) {
        monitoredServiceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MonitoredService> findServiceById(UUID id) {
        return monitoredServiceRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitoredService> findAllServices() {
        return monitoredServiceRepository.findAll();
    }

}
