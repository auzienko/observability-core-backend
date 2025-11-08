package com.auzienko.observability.corebackend.api.controller;

import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;
import com.auzienko.observability.corebackend.domain.service.LoadTester;
import com.auzienko.observability.corebackend.publicapi.dto.LoadTestRequest;
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoadTestControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private LoadTester loadTester;

    @InjectMocks
    private LoadTestController loadTestController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(loadTestController)
                .build();
    }

    @Test
    @DisplayName("POST /api/services/{id}/load-test should start a test and return 202 Accepted")
    void shouldStartLoadTest() throws Exception {
        // ARRANGE
        UUID serviceId = UUID.randomUUID();
        LoadTestRequest request = new LoadTestRequest();
        request.setVirtualUsers(10);
        request.setDurationSeconds(30);

        given(loadTester.startLoadTest(any(UUID.class), any(LoadTestScenario.class)))
                .willReturn(CompletableFuture.completedFuture(null));

        // ACT & ASSERT
        mockMvc.perform(post("/api/services/{serviceId}/load-test", serviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

}
