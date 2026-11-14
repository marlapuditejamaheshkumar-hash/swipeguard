package com.azentio.swipeguard.card;

import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Key is block:{cardRef} (never the PAN). Value is "1" for blocked, "0" for active. */
public class RedisBlockCache implements BlockCache {

    private static final String KEY_PREFIX = "block:";

    private final StringRedisTemplate redis;

    public RedisBlockCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<Boolean> get(String cardRef) {
        String value = redis.opsForValue().get(KEY_PREFIX + cardRef);
        return value == null ? Optional.empty() : Optional.of("1".equals(value));
    }

    @Override
    public void put(String cardRef, boolean blocked) {
        redis.opsForValue().set(KEY_PREFIX + cardRef, blocked ? "1" : "0");
    }
}