package com.auzienko.observability.corebackend.persistence.repository;

import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.persistence.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MonitoredServiceRepositoryImplIT extends BaseIntegrationTest {

    @Autowired
    private MonitoredServiceRepositoryImpl monitoredServiceRepository;

    @Test
    void shouldSaveAndFindService() {
        // ARRANGE
        MonitoredService newService = new MonitoredService();
        newService.setName("Test Service");
        newService.setHealthCheckScenario("fooBarScenario");
        newService.setPollingIntervalSeconds(60);

        // ACT
        MonitoredService savedService = monitoredServiceRepository.save(newService);

        // ASSERT
        assertThat(savedService.getId()).isNotNull();

        MonitoredService foundService = monitoredServiceRepository.findById(savedService.getId()).orElseThrow();
        assertThat(foundService.getName()).isEqualTo("Test Service");
    }

}
