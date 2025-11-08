package com.auzienko.observability.corebackend.persistence.mapper;

import com.auzienko.observability.corebackend.domain.model.HealthCheckResult;
import com.auzienko.observability.corebackend.persistence.entity.HealthCheckResultEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HealthCheckResultMapper {

    HealthCheckResultEntity toEntity(HealthCheckResult service);

    HealthCheckResult toDomain(HealthCheckResultEntity savedEntity);
}
