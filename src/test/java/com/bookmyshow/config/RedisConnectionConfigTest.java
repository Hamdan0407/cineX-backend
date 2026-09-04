package com.bookmyshow.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisConnectionConfigTest {

    private final RedisConnectionConfig config = new RedisConnectionConfig();

    @Test
    void redisUrlOverridesLocalHostAndPort() {
        RedisConnectionProperties properties = new RedisConnectionProperties();
        properties.setUrl("redis://app-user:example-password@redis.internal:6381/3");
        properties.setHost("localhost");
        properties.setPort(6379);

        LettuceConnectionFactory factory = config.redisConnectionFactory(properties);

        assertEquals("redis.internal", factory.getHostName());
        assertEquals(6381, factory.getPort());
        assertEquals(3, factory.getDatabase());
        assertEquals("app-user", factory.getStandaloneConfiguration().getUsername());
        assertEquals("example-password", new String(factory.getStandaloneConfiguration().getPassword().get()));
    }

    @Test
    void redisUrlWithPasswordOnlyAndUrlDecoding() {
        RedisConnectionProperties properties = new RedisConnectionProperties();
        properties.setUrl("redis://:p%40ssword123@redis.internal:6379/1");

        LettuceConnectionFactory factory = config.redisConnectionFactory(properties);

        assertEquals("redis.internal", factory.getHostName());
        assertEquals(6379, factory.getPort());
        assertEquals(1, factory.getDatabase());
        assertEquals(null, factory.getStandaloneConfiguration().getUsername());
        assertEquals("p@ssword123", new String(factory.getStandaloneConfiguration().getPassword().get()));
    }

    @Test
    void redissUrlEnablesTls() {
        RedisConnectionProperties properties = new RedisConnectionProperties();
        properties.setUrl("rediss://redis.internal:6380/0");

        RedisConnectionConfig.ResolvedRedisConnection resolved = RedisConnectionConfig.resolve(properties);

        assertTrue(resolved.ssl());
        assertEquals("redis.internal:6380/0 (TLS)", resolved.safeEndpoint());
    }

    @Test
    void redisUrlWithExplicitSslFlagEnablesTls() {
        RedisConnectionProperties properties = new RedisConnectionProperties();
        properties.setUrl("redis://redis.internal:6379/0");
        properties.setSslEnabled(true);

        RedisConnectionConfig.ResolvedRedisConnection resolved = RedisConnectionConfig.resolve(properties);

        assertTrue(resolved.ssl());
        assertEquals("redis.internal:6379/0 (TLS)", resolved.safeEndpoint());
    }

    @Test
    void localhostIsUsedOnlyWhenRedisUrlIsAbsent() {
        RedisConnectionProperties properties = new RedisConnectionProperties();
        properties.setHost("localhost");
        properties.setPort(6379);

        RedisConnectionConfig.ResolvedRedisConnection resolved = RedisConnectionConfig.resolve(properties);

        assertEquals("localhost", resolved.host());
        assertEquals(6379, resolved.port());
        assertFalse(resolved.ssl());
        assertEquals("localhost:6379/0", resolved.safeEndpoint());
    }
}
