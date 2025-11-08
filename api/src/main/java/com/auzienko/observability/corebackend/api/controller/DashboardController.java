package com.auzienko.observability.corebackend.api.controller;

import com.auzienko.observability.corebackend.api.mapper.MonitoredServiceApiMapper;
import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;
import com.auzienko.observability.corebackend.domain.service.DashboardService;
import com.auzienko.observability.corebackend.publicapi.dto.MonitoredServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final MonitoredServiceApiMapper mapper;

    @GetMapping("/status")
    public ResponseEntity<List<MonitoredServiceResponse>> getDashboardStatus() {
        List<MonitoredServiceResponse> response = dashboardService.getCurrentServicesStatus().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/services/{serviceId}/history")
    public ResponseEntity<List<HealthCheckResult>> getHealthHistory(
            @PathVariable UUID serviceId,
            @RequestParam(defaultValue = "1h") String range) { // e.g., ?range=1h, 24h, 7d

        Duration duration = parseRange(range);
        List<HealthCheckResult> history = dashboardService.getHealthHistoryForService(serviceId, duration);
        return ResponseEntity.ok(history);
    }

    private Duration parseRange(String range) {
        if (range.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(range.replace("h", "")));
        }
        if (range.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(range.replace("d", "")));
        }
        return Duration.ofHours(1); // Default
    }

}
