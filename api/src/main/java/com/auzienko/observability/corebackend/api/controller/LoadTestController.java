package com.auzienko.observability.corebackend.api.controller;

import com.auzienko.observability.corebackend.domain.model.LoadTestScenario;
import com.auzienko.observability.corebackend.loadtester.service.LoadTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/services/{serviceId}/load-test")
@RequiredArgsConstructor
public class LoadTestController {

    private final LoadTestService loadTester;

    @PostMapping
    public ResponseEntity<Void> startLoadTest(
            @PathVariable UUID serviceId,
            @Valid @RequestBody LoadTestScenario scenario) {

        loadTester.executeAndSave(serviceId, scenario);

        return ResponseEntity.accepted().build();
    }

}
