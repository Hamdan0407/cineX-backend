package com.bookmyshow.config;

import com.bookmyshow.cache.StatsTrackingCacheManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Resilient Cache Configuration for CineX.
 * Automatically attempts to connect to Redis for enterprise distributed caching.
 * If Redis server is not running (e.g., local development without Docker), it gracefully
 * falls back to an embedded ConcurrentMapCacheManager without crashing the application.
 * All cache instances are wrapped in StatsTrackingCacheManager for KPI monitoring.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> connectionFactoryProvider) {
        RedisConnectionFactory connectionFactory = connectionFactoryProvider.getIfAvailable();

        if (connectionFactory != null) {
            try {
                // Test connection
                try (RedisConnection connection = connectionFactory.getConnection()) {
                    String ping = connection.ping();
                    log.info("Redis server connection successful: ping -> {}", ping);
                }

                // Configure Redis serialization and TTLs
                ObjectMapper redisMapper = new ObjectMapper();
                redisMapper.registerModule(new JavaTimeModule());
                redisMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(redisMapper);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(60)) // Default TTL 60 mins
                        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                        .disableCachingNullValues();

                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
                // TMDB API calls cached for 60 minutes
                cacheConfigurations.put("tmdbNowPlaying", defaultConfig.entryTtl(Duration.ofMinutes(60)));
                cacheConfigurations.put("tmdbTrending", defaultConfig.entryTtl(Duration.ofMinutes(60)));
                cacheConfigurations.put("tmdbUpcoming", defaultConfig.entryTtl(Duration.ofMinutes(60)));
                cacheConfigurations.put("tmdbMovieDetails", defaultConfig.entryTtl(Duration.ofMinutes(60)));
                cacheConfigurations.put("tmdbMovieCredits", defaultConfig.entryTtl(Duration.ofMinutes(60)));
                cacheConfigurations.put("tmdbMovieSimilar", defaultConfig.entryTtl(Duration.ofMinutes(60)));
                // Database entity caches
                cacheConfigurations.put("movies", defaultConfig.entryTtl(Duration.ofMinutes(30)));
                cacheConfigurations.put("movieDetails", defaultConfig.entryTtl(Duration.ofMinutes(30)));
                cacheConfigurations.put("theatres", defaultConfig.entryTtl(Duration.ofMinutes(30)));
                cacheConfigurations.put("cities", defaultConfig.entryTtl(Duration.ofHours(6)));
                cacheConfigurations.put("shows", defaultConfig.entryTtl(Duration.ofMinutes(15)));

                RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                        .cacheDefaults(defaultConfig)
                        .withInitialCacheConfigurations(cacheConfigurations)
                        .build();

                log.info("Using RedisCacheManager wrapped in StatsTrackingCacheManager.");
                return new StatsTrackingCacheManager(redisCacheManager, "REDIS");

            } catch (Exception ex) {
                log.warn("Redis server unreachable ({}). Falling back to embedded ConcurrentMapCacheManager.", ex.getMessage());
            }
        } else {
            log.info("RedisConnectionFactory not available. Using embedded ConcurrentMapCacheManager.");
        }

        ConcurrentMapCacheManager mapCacheManager = new ConcurrentMapCacheManager();
        return new StatsTrackingCacheManager(mapCacheManager, "EMBEDDED_CONCURRENT_MAP");
    }
}
