package com.auzienko.observability.corebackend.persistence.repository;

import com.auzienko.observability.corebackend.persistence.entity.HealthCheckResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HealthCheckResultJpaRepository extends JpaRepository<HealthCheckResultEntity, UUID> {

    @Query("select h from HealthCheckResultEntity h where h.serviceId = ?1 and h.timestamp >= ?2")
    List<HealthCheckResultEntity> findHistoryByServiceIdSince(UUID serviceId, Instant timestamp);

}
