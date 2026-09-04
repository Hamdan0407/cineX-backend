package com.bookmyshow.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Creates the one Redis factory shared by cache and distributed seat locking. */
@Configuration
@EnableConfigurationProperties(RedisConnectionProperties.class)
public class RedisConnectionConfig {

    @Bean
    @ConditionalOnMissingBean(LettuceConnectionFactory.class)
    public LettuceConnectionFactory redisConnectionFactory(RedisConnectionProperties properties) {
        ResolvedRedisConnection resolved = resolve(properties);
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(resolved.host(), resolved.port());
        standalone.setDatabase(resolved.database());
        if (!resolved.username().isBlank()) standalone.setUsername(resolved.username());
        if (!resolved.password().isBlank()) standalone.setPassword(RedisPassword.of(resolved.password()));

        LettuceClientConfiguration.LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder()
                .commandTimeout(properties.getTimeout() == null ? Duration.ofSeconds(2) : properties.getTimeout());
        if (resolved.ssl()) client.useSsl();
        return new LettuceConnectionFactory(standalone, client.build());
    }

    static ResolvedRedisConnection resolve(RedisConnectionProperties properties) {
        if (!properties.hasUrl()) {
            return new ResolvedRedisConnection(properties.getHost(), properties.getPort(), 0,
                    properties.getUsername(), properties.getPassword(), properties.isSslEnabled());
        }
        try {
            URI uri = new URI(properties.getUrl());
            String scheme = uri.getScheme();
            if (!"redis".equalsIgnoreCase(scheme) && !"rediss".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("REDIS_URL must use redis:// or rediss://");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("REDIS_URL must include a Redis host");
            }
            Credentials credentials = credentials(uri.getRawUserInfo(), properties);
            return new ResolvedRedisConnection(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 6379,
                    database(uri.getPath()), credentials.username(), credentials.password(),
                    "rediss".equalsIgnoreCase(scheme) || properties.isSslEnabled());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("REDIS_URL is not a valid Redis URI", ex);
        }
    }

    private static Credentials credentials(String rawUserInfo, RedisConnectionProperties properties) {
        if (rawUserInfo == null || rawUserInfo.isBlank()) return new Credentials(properties.getUsername(), properties.getPassword());
        String decoded = URLDecoder.decode(rawUserInfo, StandardCharsets.UTF_8);
        int separator = decoded.indexOf(':');
        return separator < 0 ? new Credentials(decoded, properties.getPassword())
                : new Credentials(decoded.substring(0, separator), decoded.substring(separator + 1));
    }

    private static int database(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) return 0;
        try { return Integer.parseInt(path.substring(1)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("REDIS_URL database must be numeric", ex); }
    }

    record ResolvedRedisConnection(String host, int port, int database, String username, String password, boolean ssl) {
        String safeEndpoint() { return host + ":" + port + "/" + database + (ssl ? " (TLS)" : ""); }
    }
    private record Credentials(String username, String password) { }
}
