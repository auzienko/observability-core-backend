package com.auzienko.observability.corebackend.api.mapper;

import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.publicapi.dto.MonitoredServiceRequest;
import com.auzienko.observability.corebackend.publicapi.dto.MonitoredServiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MonitoredServiceApiMapper {

    MonitoredService toDomain(MonitoredServiceRequest request);

    @Mapping(target = "status", expression = "java(service.getStatus() != null ? service.getStatus().name() : null)")
    MonitoredServiceResponse toResponse(MonitoredService service);

}
