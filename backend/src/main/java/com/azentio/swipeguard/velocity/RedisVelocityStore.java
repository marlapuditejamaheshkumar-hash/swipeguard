package com.azentio.swipeguard.velocity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** Sorted set per card. Key is vel:{cardRef}, never the PAN. Trim + add + count run in one Lua script. */
public class RedisVelocityStore implements VelocityStore {

    private static final String KEY_PREFIX = "vel:";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> script = new DefaultRedisScript<>();

    public RedisVelocityStore(StringRedisTemplate redis) {
        this.redis = redis;
        script.setLocation(new ClassPathResource("scripts/velocity.lua"));
        script.setResultType(Long.class);
    }

    @Override
    public long recordAndCount(String cardRef, String transactionId, Instant at, Duration window) {
        long timestamp = at.toEpochMilli();
        Long count = redis.execute(script,
                List.of(KEY_PREFIX + cardRef),
                String.valueOf(timestamp - window.toMillis()),
                String.valueOf(timestamp),
                transactionId,
                String.valueOf(window.toMillis()));
        return count == null ? 0 : count;
    }

    @Override
    public long size(String cardRef) {
        Long size = redis.opsForZSet().zCard(KEY_PREFIX + cardRef);
        return size == null ? 0 : size;
    }
}