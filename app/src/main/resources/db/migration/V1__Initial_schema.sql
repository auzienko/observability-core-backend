CREATE TABLE monitored_services (
    id                       UUID PRIMARY KEY,
    name                     VARCHAR(255) NOT NULL,
    health_check_scenario    jsonb NOT NULL,
    status                   VARCHAR(20),
    last_checked_at          TIMESTAMP WITHOUT TIME ZONE,
    updated_at               TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE health_check_results (
    id                UUID PRIMARY KEY,
    service_id        UUID NOT NULL,
    timestamp         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status            VARCHAR(20) NOT NULL,
    error_message     TEXT,
    CONSTRAINT fk_health_check_service FOREIGN KEY (service_id) REFERENCES monitored_services (id) ON DELETE CASCADE
);

CREATE INDEX idx_service_id_timestamp_desc ON health_check_results (service_id, timestamp DESC);

CREATE TABLE load_test_results (
    id                    UUID PRIMARY KEY,
    service_id            UUID NOT NULL,
    executed_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    total_requests        BIGINT NOT NULL,
    successful_requests   BIGINT NOT NULL,
    requests_per_second   DOUBLE PRECISION NOT NULL,
    avg_response_time_ms  BIGINT NOT NULL,
    p95_response_time_ms  BIGINT NOT NULL,
    p99_response_time_ms  BIGINT NOT NULL,
    CONSTRAINT fk_load_test_service FOREIGN KEY (service_id) REFERENCES monitored_services (id) ON DELETE CASCADE
);
