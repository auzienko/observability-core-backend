package com.auzienko.observability.corebackend.api.mapper;

import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;
import com.auzienko.observability.corebackend.publicapi.dto.HealthCheckResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HealthCheckApiMapper {

    @Mapping(target = "status", expression = "java(healthCheckResult.getStatus() != null ? healthCheckResult.getStatus().name() : null)")
    HealthCheckResponse toResponse(HealthCheckResult healthCheckResult);

}
