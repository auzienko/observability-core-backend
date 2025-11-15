package com.auzienko.observability.corebackend.loadtester.model.consumer;

import com.auzienko.observability.corebackend.loadtester.model.RawRequestResult;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Composite consumer that broadcasts to multiple consumers.
 * Allows combining different behaviors.
 */
public class CompositeConsumer implements Consumer<RawRequestResult> {

    private final List<Consumer<RawRequestResult>> consumers;

    @SafeVarargs
    public CompositeConsumer(Consumer<RawRequestResult>... consumers) {
        this.consumers = Arrays.asList(consumers);
    }

    @Override
    public void accept(RawRequestResult result) {
        for (Consumer<RawRequestResult> consumer : consumers) {
            try {
                consumer.accept(result);
            } catch (Exception e) {
                System.err.println("Consumer failed: " + e.getMessage());
            }
        }
    }

}
