package com.auzienko.observability.corebackend.persistence.mapper;

import com.auzienko.observability.corebackend.domain.model.LoadTestResult;
import com.auzienko.observability.corebackend.persistence.entity.LoadTestResultEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoadTestResultMapper {

    LoadTestResultEntity toEntity(LoadTestResult service);

    LoadTestResult toDomain(LoadTestResultEntity savedEntity);

}
