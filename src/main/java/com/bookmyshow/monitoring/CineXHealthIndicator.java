package com.bookmyshow.monitoring;

import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CineXHealthIndicator implements HealthIndicator {

    private final MovieRepository movieRepository;
    private final BookingRepository bookingRepository;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean dbHealthy = false;

        try {
            long movieCount = movieRepository.count();
            long bookingCount = bookingRepository.count();
            details.put("database", "UP");
            details.put("moviesInCatalog", movieCount);
            details.put("totalBookingsRecorded", bookingCount);
            dbHealthy = true;
        } catch (Exception e) {
            log.error("HealthCheck failed for Database: {}", e.getMessage());
            details.put("database", "DOWN (" + e.getMessage() + ")");
        }

        RedisConnectionFactory connectionFactory = redisConnectionFactoryProvider.getIfAvailable();
        if (connectionFactory != null) {
            try (RedisConnection connection = connectionFactory.getConnection()) {
                String pong = connection.ping();
                if ("PONG".equalsIgnoreCase(pong)) {
                    details.put("redisCache", "UP");
                } else {
                    details.put("redisCache", "DEGRADED (" + pong + ")");
                }
            } catch (Exception e) {
                log.warn("HealthCheck indicates Redis Cache is offline: {}", e.getMessage());
                details.put("redisCache", "DOWN (Fallback to Embedded ConcurrentMap active)");
            }
        } else {
            details.put("redisCache", "EMBEDDED (ConcurrentMap active)");
        }

        details.put("razorpayGateway", "CONFIGURED (Test Mode Active)");
        details.put("systemStatus", dbHealthy ? "OPTIMAL" : "CRITICAL");

        if (dbHealthy) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }
}
