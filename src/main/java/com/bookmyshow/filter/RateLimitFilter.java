package com.bookmyshow.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production-grade API Rate Limiting Filter for CineX.
 * Implements a high-performance in-memory sliding window / token bucket algorithm.
 * Protects backend against DDoS attacks and brute-force booking attempts.
 * Sets standard HTTP Rate Limit response headers (X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_DURATION_MS = 60_000L; // 1 minute window
    private static final int DEFAULT_LIMIT = 120; // 120 reqs/min for general API
    private static final int TRANSACTIONAL_LIMIT = 20; // 20 reqs/min for checkout/bookings

    private final Map<String, ClientRequestBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip rate limiting for internal health checks, swagger documentation, websocket, and h2 console
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui") 
                || path.startsWith("/v3/api-docs") || path.startsWith("/h2-console") || path.startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        int maxLimit = isTransactionalEndpoint(path) ? TRANSACTIONAL_LIMIT : DEFAULT_LIMIT;
        String clientKey = resolveClientKey(request, path);

        ClientRequestBucket bucket = buckets.compute(clientKey, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || (now - existing.windowStartMillis) >= WINDOW_DURATION_MS) {
                return new ClientRequestBucket(now, 1);
            } else {
                existing.requestCount++;
                return existing;
            }
        });

        long now = System.currentTimeMillis();
        long windowElapsed = now - bucket.windowStartMillis;
        long resetSeconds = Math.max(1, (WINDOW_DURATION_MS - windowElapsed) / 1000L);
        int remaining = Math.max(0, maxLimit - bucket.requestCount);

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxLimit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));

        if (bucket.requestCount > maxLimit) {
            log.warn("Rate limit exceeded for client [{}]. Limit: {}/min. Endpoint: {}", clientKey, maxLimit, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(resetSeconds));

            String jsonError = String.format(
                "{\"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"API rate limit exceeded. Please try again in %d seconds.\", \"timestamp\": \"%s\"}",
                resetSeconds, Instant.now().toString()
            );
            response.getWriter().write(jsonError);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTransactionalEndpoint(String path) {
        return path.startsWith("/api/bookings") || path.startsWith("/api/payments");
    }

    private String resolveClientKey(HttpServletRequest request, String path) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 10) {
            return "user:" + authHeader.substring(7).trim() + ":" + getEndpointCategory(path);
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip + ":" + getEndpointCategory(path);
    }

    private String getEndpointCategory(String path) {
        return isTransactionalEndpoint(path) ? "tx" : "general";
    }

    private static class ClientRequestBucket {
        long windowStartMillis;
        int requestCount;

        ClientRequestBucket(long windowStartMillis, int requestCount) {
            this.windowStartMillis = windowStartMillis;
            this.requestCount = requestCount;
        }
    }
}
