package com.auzienko.observability.corebackend.persistence.repository;

import com.auzienko.observability.corebackend.persistence.entity.MonitoredServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MonitoredServiceJpaRepository extends JpaRepository<MonitoredServiceEntity, UUID> {
}