package com.auzienko.observability.corebackend.api.controller;

import com.auzienko.observability.corebackend.api.mapper.MonitoredServiceApiMapper;
import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.service.DashboardService;
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

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private DashboardService dashboardService;

    @Mock
    private MonitoredServiceApiMapper mapper;

    @InjectMocks
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .build();
    }

    @Test
    @DisplayName("GET /api/dashboard/status should return a list of service statuses")
    void shouldReturnDashboardStatus() throws Exception {
        // ARRANGE
        given(dashboardService.getCurrentServicesStatus()).willReturn(List.of(new MonitoredService(), new MonitoredService()));

        // ACT & ASSERT
        mockMvc.perform(get("/api/dashboard/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/dashboard/services/{id}/history should return health history for a service")
    void shouldReturnHealthHistory() throws Exception {
        // ARRANGE
        UUID serviceId = UUID.randomUUID();
        given(dashboardService.getHealthHistoryForService(any(UUID.class), any(Duration.class)))
                .willReturn(List.of());

        // ACT & ASSERT
        mockMvc.perform(get("/api/dashboard/services/{serviceId}/history", serviceId)
                        .param("range", "24h")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

}
