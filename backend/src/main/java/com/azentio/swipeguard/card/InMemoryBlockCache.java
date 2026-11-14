package com.azentio.swipeguard.card;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBlockCache implements BlockCache {

    private final Map<String, Boolean> states = new ConcurrentHashMap<>();

    @Override
    public Optional<Boolean> get(String cardRef) {
        return Optional.ofNullable(states.get(cardRef));
    }

    @Override
    public void put(String cardRef, boolean blocked) {
        states.put(cardRef, blocked);
    }
}