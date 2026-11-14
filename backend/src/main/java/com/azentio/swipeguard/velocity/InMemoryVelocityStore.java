package com.azentio.swipeguard.velocity;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Same behaviour as the Redis version, used by unit tests so no Docker is needed. */
public class InMemoryVelocityStore implements VelocityStore {

    private final Map<String, Map<String, Long>> windows = new HashMap<>();

    @Override
    public synchronized long recordAndCount(String cardRef, String transactionId, Instant at, Duration window) {
        Map<String, Long> entries = windows.computeIfAbsent(cardRef, k -> new HashMap<>());
        long windowStart = at.minus(window).toEpochMilli();
        entries.values().removeIf(timestamp -> timestamp < windowStart);
        entries.put(transactionId, at.toEpochMilli());
        return entries.size();
    }

    @Override
    public synchronized long size(String cardRef) {
        return windows.getOrDefault(cardRef, Map.of()).size();
    }
}