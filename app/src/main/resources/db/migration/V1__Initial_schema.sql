CREATE TABLE IF NOT EXISTS monitored_services (
    id                       UUID PRIMARY KEY,
    name                     VARCHAR(255) NOT NULL,
    health_check_scenario    jsonb NOT NULL,
    polling_interval_seconds INT NOT NULL,
    status                   VARCHAR(20),
    last_checked_at          TIMESTAMP WITHOUT TIME ZONE,
    updated_at               TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS health_check_results (
    id                UUID PRIMARY KEY,
    service_id        UUID NOT NULL,
    timestamp         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status            VARCHAR(20) NOT NULL,
    http_status_code  INT,
    response_time_ms  BIGINT,
    CONSTRAINT fk_health_check_service FOREIGN KEY (service_id) REFERENCES monitored_services (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_service_id_timestamp_desc ON health_check_results (service_id, timestamp DESC);

CREATE TABLE IF NOT EXISTS load_test_results (
    id                UUID PRIMARY KEY,
    service_id        UUID NOT NULL,
    timestamp         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status            VARCHAR(20) NOT NULL,
    http_status_code  INT,
    response_time_ms  BIGINT,
    CONSTRAINT fk_health_check_service FOREIGN KEY (service_id) REFERENCES monitored_services (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_service_id_timestamp_desc ON load_test_results (service_id, timestamp DESC);