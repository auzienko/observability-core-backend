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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "monitored_services")
public class MonitoredServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    private String healthCheckScenario;

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    private Instant lastCheckedAt;

    @UpdateTimestamp
    private Instant updatedAt;

}
