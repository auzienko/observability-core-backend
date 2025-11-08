package com.auzienko.observability.corebackend.api.controller;

import com.auzienko.observability.corebackend.api.mapper.MonitoredServiceApiMapper;
import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.service.ServiceRegistry;
import com.auzienko.observability.corebackend.publicapi.dto.MonitoredServiceRequest;
import com.auzienko.observability.corebackend.publicapi.dto.MonitoredServiceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private ServiceRegistry serviceRegistry;

    @Mock
    private MonitoredServiceApiMapper mapper;

    @InjectMocks
    private ServiceRegistryController serviceRegistryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(serviceRegistryController)
                .build();
    }

    @Test
    @DisplayName("POST /api/services should create a new monitored service and return 201 Created")
    void shouldCreateNewMonitoredService() throws Exception {
        // ARRANGE
        MonitoredServiceRequest requestDto = new MonitoredServiceRequest();
        requestDto.setName("New Service");
        requestDto.setHealthCheckScenario("fooBarScenario");
        requestDto.setPollingIntervalSeconds(30);

        UUID serviceId = UUID.randomUUID();
        MonitoredService domainObject = new MonitoredService();
        MonitoredService savedDomainObject = new MonitoredService();
        savedDomainObject.setId(serviceId);
        savedDomainObject.setName("New Service");

        MonitoredServiceResponse responseDto = new MonitoredServiceResponse();
        responseDto.setId(serviceId);
        responseDto.setName("New Service");

        given(mapper.toDomain(any(MonitoredServiceRequest.class))).willReturn(domainObject);
        given(mapper.toResponse(savedDomainObject)).willReturn(responseDto);
        given(serviceRegistry.registerService(domainObject)).willReturn(savedDomainObject);

        // ACT & ASSERT
        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(savedDomainObject.getId().toString()))
                .andExpect(jsonPath("$.name").value("New Service"));
    }

    @Test
    @DisplayName("GET /api/services/{id} should return service when found")
    void shouldReturnServiceById() throws Exception {
        // ARRANGE
        UUID serviceId = UUID.randomUUID();
        MonitoredService service = new MonitoredService();
        service.setId(serviceId);
        service.setName("Found Service");

        given(serviceRegistry.findServiceById(serviceId)).willReturn(Optional.of(service));
        given(mapper.toResponse(any(MonitoredService.class))).willReturn(new MonitoredServiceResponse());

        // ACT & ASSERT
        mockMvc.perform(get("/api/services/{id}", serviceId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/services/{id} should return 404 when not found")
    void shouldReturn404WhenServiceNotFound() throws Exception {
        // ARRANGE
        UUID serviceId = UUID.randomUUID();
        given(serviceRegistry.findServiceById(serviceId)).willReturn(Optional.empty());

        // ACT & ASSERT
        mockMvc.perform(get("/api/services/{id}", serviceId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/services/{id} should return 204 No Content")
    void shouldDeleteService() throws Exception {
        // ARRANGE
        UUID serviceId = UUID.randomUUID();
        willDoNothing().given(serviceRegistry).unregisterService(serviceId);

        // ACT & ASSERT
        mockMvc.perform(delete("/api/services/{id}", serviceId))
                .andExpect(status().isNoContent());

        verify(serviceRegistry).unregisterService(serviceId);
    }

}
