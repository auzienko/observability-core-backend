package com.auzienko.observability.corebackend.api.controller;

import com.auzienko.observability.corebackend.api.mapper.MonitoredServiceApiMapper;
import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.domain.service.ServiceRegistry;
import com.auzienko.observability.corebackend.publicapi.dto.MonitoredServiceRequest;
import com.auzienko.observability.corebackend.publicapi.dto.MonitoredServiceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceRegistryController {

    private final ServiceRegistry serviceRegistry;
    private final MonitoredServiceApiMapper mapper;

    @PostMapping
    public ResponseEntity<MonitoredServiceResponse> registerService(
            @Valid @RequestBody MonitoredServiceRequest request) {

        MonitoredService domainToRegister = mapper.toDomain(request);
        MonitoredService registeredService = serviceRegistry.registerService(domainToRegister);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(registeredService.getId())
                .toUri();

        MonitoredServiceResponse responseDto = mapper.toResponse(registeredService);

        return ResponseEntity.created(location).body(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<MonitoredServiceResponse>> getAllServices() {
        List<MonitoredServiceResponse> responses = serviceRegistry.findAllServices().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitoredServiceResponse> getServiceById(@PathVariable UUID id) {
        return serviceRegistry.findServiceById(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitoredServiceResponse> updateService(
            @PathVariable UUID id,
            @Valid @RequestBody MonitoredServiceRequest request) {

        MonitoredService serviceUpdate = mapper.toDomain(request);
        MonitoredService updatedService = serviceRegistry.updateService(id, serviceUpdate);

        return ResponseEntity.ok(mapper.toResponse(updatedService));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterService(@PathVariable UUID id) {
        serviceRegistry.unregisterService(id);
    }

}
