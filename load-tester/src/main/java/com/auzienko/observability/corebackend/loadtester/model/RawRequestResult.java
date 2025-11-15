package com.auzienko.observability.corebackend.loadtester.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class RawRequestResult {

    // Execution context
    private int workerId;
    private int iteration;
    private int stepIndex;
    private String stepName;

    // Request data
    private String url;
    private String method;
    private Map<String, String> requestHeaders;
    private String requestBody;

    // Timing
    private Instant startTime;
    private long durationNanos;

    // Response data
    private Integer statusCode;
    private Map<String, String> responseHeaders;
    private String responseBody;

    // Status
    private boolean success;
    private String errorType;
    private String errorMessage;

    // Util methods
    public long getDurationMs() {
        return durationNanos / 1_000_000;
    }

    public boolean isHttpError() {
        return statusCode != null && (statusCode >= 400);
    }

    public boolean isServerError() {
        return statusCode != null && (statusCode >= 500);
    }

    public boolean isClientError() {
        return statusCode != null && (statusCode >= 400 && statusCode < 500);
    }

}
