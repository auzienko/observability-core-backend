package com.auzienko.observability.corebackend.loadtester.model.consumer;

import com.auzienko.observability.corebackend.loadtester.model.RawRequestResult;

import java.util.function.Consumer;

/**
 * Debug consumer that logs detailed information.
 * Useful for troubleshooting failed tests.
 */
public class DebugConsumer implements Consumer<RawRequestResult> {
    private final boolean logSuccessful;
    private final boolean logRequestBody;
    private final boolean logResponseBody;

    public DebugConsumer(boolean logSuccessful, boolean logRequestBody, boolean logResponseBody) {
        this.logSuccessful = logSuccessful;
        this.logRequestBody = logRequestBody;
        this.logResponseBody = logResponseBody;
    }

    @Override
    public void accept(RawRequestResult result) {
        if (!result.isSuccess() || logSuccessful) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[Worker %d, Iter %d, Step %d] %s %s - %s (%dms)",
                    result.getWorkerId(),
                    result.getIteration(),
                    result.getStepIndex(),
                    result.getMethod(),
                    result.getUrl(),
                    result.isSuccess() ? "SUCCESS" : "FAILED",
                    result.getDurationMs()
            ));

            if (!result.isSuccess()) {
                sb.append(String.format(" [%s: %s]", result.getErrorType(), result.getErrorMessage()));
            }

            if (result.getStatusCode() != null) {
                sb.append(String.format(" Status: %d", result.getStatusCode()));
            }

            if (logRequestBody && result.getRequestBody() != null) {
                sb.append(String.format("\n  Request: %s", truncate(result.getRequestBody(), 200)));
            }

            if (logResponseBody && result.getResponseBody() != null) {
                sb.append(String.format("\n  Response: %s", truncate(result.getResponseBody(), 200)));
            }

            System.out.println(sb.toString());
        }
    }

    private String truncate(String str, int maxLen) {
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

}
