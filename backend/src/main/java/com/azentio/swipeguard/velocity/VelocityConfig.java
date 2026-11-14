package com.azentio.swipeguard.velocity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class VelocityConfig {

    @Bean
    @ConditionalOnProperty(name = "swipeguard.velocity-store", havingValue = "redis", matchIfMissing = true)
    VelocityStore redisVelocityStore(StringRedisTemplate redis) {
        return new RedisVelocityStore(redis);
    }

    @Bean
    @ConditionalOnProperty(name = "swipeguard.velocity-store", havingValue = "memory")
    VelocityStore inMemoryVelocityStore() {
        return new InMemoryVelocityStore();
    }
}