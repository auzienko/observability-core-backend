package com.auzienko.observability.corebackend.persistence.service;

import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryImplTest {

    @Mock
    private MonitoredServiceRepository monitoredServiceRepository;

    @InjectMocks
    private ServiceRegistryImpl serviceRegistry;

    @Test
    @DisplayName("should save the service using the repository when registering a new service")
    void registerService_shouldSaveService() {
        // ARRANGE
        MonitoredService newService = new MonitoredService();
        newService.setName("New Test Service");

        given(monitoredServiceRepository.save(any(MonitoredService.class)))
                .willAnswer(invocation -> {
                    MonitoredService serviceToSave = invocation.getArgument(0);
                    serviceToSave.setId(UUID.randomUUID()); // Simulate ID generation
                    return serviceToSave;
                });

        // ACT
        MonitoredService registeredService = serviceRegistry.registerService(newService);

        // ASSERT
        assertThat(registeredService.getId()).isNotNull();
        assertThat(registeredService.getName()).isEqualTo("New Test Service");

        verify(monitoredServiceRepository).save(newService);
    }

    @Test
    @DisplayName("should call deleteById on the repository when unregistering a service")
    void unregisterService_shouldCallDelete() {
        // ARRANGE
        UUID serviceId = UUID.randomUUID();

        // ACT
        serviceRegistry.unregisterService(serviceId);

        // ASSERT
        verify(monitoredServiceRepository).deleteById(serviceId);
    }

}
