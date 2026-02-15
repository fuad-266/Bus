package com.busticket.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL of 30 minutes
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("citySuggestions", config.entryTtl(Duration.ofHours(1))) // City suggestions cache for 1 hour
                .withCacheConfiguration("departureCities", config.entryTtl(Duration.ofHours(2))) // Departure cities cache for 2 hours
                .withCacheConfiguration("destinationCities", config.entryTtl(Duration.ofHours(2))) // Destination cities cache for 2 hours
                .withCacheConfiguration("busOperators", config.entryTtl(Duration.ofHours(1))) // Bus operators cache for 1 hour
                .withCacheConfiguration("reachableCities", config.entryTtl(Duration.ofHours(1))) // Reachable cities cache for 1 hour
                .build();
    }
}