package com.azentio.swipeguard.card;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class BlockCacheConfig {

    @Bean
    @ConditionalOnProperty(name = "swipeguard.velocity-store", havingValue = "redis", matchIfMissing = true)
    BlockCache redisBlockCache(StringRedisTemplate redis) {
        return new RedisBlockCache(redis);
    }

    @Bean
    @ConditionalOnProperty(name = "swipeguard.velocity-store", havingValue = "memory")
    BlockCache inMemoryBlockCache() {
        return new InMemoryBlockCache();
    }
}