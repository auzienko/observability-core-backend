package com.auzienko.observability.corebackend.persistence.repository;

import com.auzienko.observability.corebackend.persistence.entity.LoadTestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoadTestResultJpaRepository extends JpaRepository<LoadTestResultEntity, UUID> {
}
