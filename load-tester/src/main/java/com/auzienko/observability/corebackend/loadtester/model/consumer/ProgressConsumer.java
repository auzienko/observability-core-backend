package com.auzienko.observability.corebackend.loadtester.model.consumer;

import com.auzienko.observability.corebackend.loadtester.model.RawRequestResult;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Real-time progress consumer.
 * Can trigger callbacks for UI updates.
 */
public class ProgressConsumer implements Consumer<RawRequestResult> {
    @Getter
    private final AtomicLong completedRequests = new AtomicLong(0);
    private final long expectedTotal;
    private final Consumer<Double> progressCallback;
    private long lastCallbackTime = 0;
    private final long callbackIntervalMs;

    public ProgressConsumer(long expectedTotal, Consumer<Double> progressCallback, long callbackIntervalMs) {
        this.expectedTotal = expectedTotal;
        this.progressCallback = progressCallback;
        this.callbackIntervalMs = callbackIntervalMs;
    }

    @Override
    public void accept(RawRequestResult result) {
        long completed = completedRequests.incrementAndGet();

        long now = System.currentTimeMillis();
        if (now - lastCallbackTime >= callbackIntervalMs) {
            lastCallbackTime = now;
            double progress = expectedTotal > 0 ? (completed * 100.0) / expectedTotal : 0.0;
            progressCallback.accept(progress);
        }
    }

    public double getProgress() {
        return expectedTotal > 0 ? (completedRequests.get() * 100.0) / expectedTotal : 0.0;
    }

}
