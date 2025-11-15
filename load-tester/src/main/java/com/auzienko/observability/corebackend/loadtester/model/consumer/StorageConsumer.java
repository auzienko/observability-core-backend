package com.auzienko.observability.corebackend.loadtester.model.consumer;

import com.auzienko.observability.corebackend.loadtester.model.RawRequestResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Storage consumer that persists results to database.
 * Can be selective about what to store.
 */
public class StorageConsumer implements Consumer<RawRequestResult> {

    private final List<RawRequestResult> results = Collections.synchronizedList(new ArrayList<>());
    private final boolean storeOnlyFailures;
    private final int maxResults;

    public StorageConsumer(boolean storeOnlyFailures, int maxResults) {
        this.storeOnlyFailures = storeOnlyFailures;
        this.maxResults = maxResults;
    }

    @Override
    public void accept(RawRequestResult result) {
        if (results.size() >= maxResults) {
            return; // Don't store more than limit
        }

        if (!storeOnlyFailures || !result.isSuccess()) {
            results.add(result);
        }
    }

    public List<RawRequestResult> getResults() {
        return new ArrayList<>(results);
    }

}
