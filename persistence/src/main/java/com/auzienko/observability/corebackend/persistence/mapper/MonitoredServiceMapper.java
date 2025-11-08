package com.auzienko.observability.corebackend.persistence.mapper;

import com.auzienko.observability.corebackend.domain.model.MonitoredService;
import com.auzienko.observability.corebackend.persistence.entity.MonitoredServiceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MonitoredServiceMapper {

    MonitoredServiceEntity toEntity(MonitoredService service);

    MonitoredService toDomain(MonitoredServiceEntity savedEntity);

}
