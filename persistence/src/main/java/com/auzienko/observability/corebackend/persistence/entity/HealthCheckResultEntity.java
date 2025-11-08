package com.auzienko.observability.corebackend.persistence.entity;

import com.auzienko.observability.corebackend.domain.model.ServiceStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "health_check_results")
public class HealthCheckResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID serviceId;

    @CreationTimestamp
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    private Integer httpStatusCode;
    private Long responseTimeMs;

}
