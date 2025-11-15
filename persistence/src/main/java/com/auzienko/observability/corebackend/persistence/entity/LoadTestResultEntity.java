package com.auzienko.observability.corebackend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "load_test_results")
public class LoadTestResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID serviceId;

    @CreationTimestamp
    private Instant executedAt;

    private Long totalRequests;

    private Long successfulRequests;

    private Double requestsPerSecond;

    private Long avgResponseTimeMs;

    @Column(name = "p95_response_time_ms")
    private Long p95ResponseTimeMs;

    @Column(name = "p99_response_time_ms")
    private Long p99ResponseTimeMs;

}
