package com.bookmyshow.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for Redis-backed seat locking.
 * Uses Testcontainers when Docker is available from the JVM; otherwise falls back to localhost:6379
 * (e.g. docker-compose redis).
 */
class SeatLockServiceRedisIntegrationTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");
    private static final String LOCAL_REDIS_HOST = System.getProperty("cinex.test.redis.host", "localhost");
    private static final int LOCAL_REDIS_PORT = Integer.getInteger("cinex.test.redis.port", 6379);

    @SuppressWarnings("resource")
    private static GenericContainer<?> testContainer;

    private StringRedisTemplate redisTemplate;
    private LettuceConnectionFactory connectionFactory;
    private SeatLockService seatLockService;

    @BeforeEach
    void setUp() {
        RedisEndpoint endpoint = resolveRedisEndpoint();
        assumeTrue(endpoint != null, "Redis is required for integration tests (Testcontainers Docker or localhost:6379)");

        connectionFactory = new LettuceConnectionFactory(endpoint.host(), endpoint.port());
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        seatLockService = new SeatLockService(Mockito.mock(SimpMessagingTemplate.class), redisTemplate);
        ReflectionTestUtils.setField(seatLockService, "backend", "redis");
        ReflectionTestUtils.setField(seatLockService, "holdTtl", Duration.ofSeconds(2));
    }

    private RedisEndpoint resolveRedisEndpoint() {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            try {
                if (testContainer == null) {
                    testContainer = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);
                }
                if (!testContainer.isRunning()) {
                    testContainer.start();
                }
                return new RedisEndpoint(testContainer.getHost(), testContainer.getMappedPort(6379), false);
            } catch (RuntimeException ignored) {
                // Fall through to localhost when Testcontainers cannot start a container.
            }
        }
        return pingLocalRedis() ? new RedisEndpoint(LOCAL_REDIS_HOST, LOCAL_REDIS_PORT, true) : null;
    }

    private boolean pingLocalRedis() {
        LettuceConnectionFactory probe = new LettuceConnectionFactory(LOCAL_REDIS_HOST, LOCAL_REDIS_PORT);
        try {
            probe.afterPropertiesSet();
            String pong = probe.getConnection().ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (RuntimeException ex) {
            return false;
        } finally {
            probe.destroy();
        }
    }

    private record RedisEndpoint(String host, int port, boolean local) {}

    @AfterEach
    void tearDown() {
        if (redisTemplate != null) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("Redis should atomically reject conflicting seat acquisition")
    void redisRejectsConflictingAcquisition() {
        assertTrue(seatLockService.holdSeats(201L, List.of(11L, 12L), "user-A", "session-a"));
        assertFalse(seatLockService.holdSeats(201L, List.of(12L, 13L), "user-B", "session-b"));

        assertEquals(List.of(11L, 12L), seatLockService.getHeldSeatIds(201L));
    }

    @Test
    @DisplayName("Redis should enforce ownership-safe release")
    void redisEnforcesOwnershipOnRelease() {
        seatLockService.holdSeats(202L, List.of(21L), "user-A", "session-a");

        seatLockService.releaseSeats(202L, List.of(21L), "user-B", "session-b");

        assertEquals(List.of(21L), seatLockService.getHeldSeatIds(202L));

        seatLockService.releaseSeats(202L, List.of(21L), "user-A", "session-a");
        assertTrue(seatLockService.getHeldSeatIds(202L).isEmpty());
    }

    @Test
    @DisplayName("Redis holds should expire after TTL")
    void redisHoldExpiresAfterTtl() throws InterruptedException {
        assertTrue(seatLockService.holdSeats(203L, List.of(31L), "user-A", "session-a"));
        assertEquals(List.of(31L), seatLockService.getHeldSeatIds(203L));

        Thread.sleep(2_500);

        assertTrue(seatLockService.getHeldSeatIds(203L).isEmpty(), "Expired Redis holds must no longer be returned");
    }

    @Test
    @DisplayName("Redis should release all seats for a disconnected session")
    void redisReleasesSeatsOnSessionDisconnect() {
        seatLockService.holdSeats(204L, List.of(41L, 42L), "user-A", "session-disconnect");
        assertEquals(2, seatLockService.getHeldSeatIds(204L).size());

        seatLockService.handleSessionDisconnect("session-disconnect");

        assertTrue(seatLockService.getHeldSeatIds(204L).isEmpty());
    }

    @Test
    @DisplayName("Redis acquisition should be safe under concurrent contention")
    void redisHandlesConcurrentAcquisition() throws InterruptedException {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final String userId = "user-" + i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (seatLockService.holdSeats(205L, List.of(50L), userId, "session-" + userId)) {
                        successes.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successes.get(), "Only one concurrent holder should win the same seat");
        assertEquals(List.of(50L), seatLockService.getHeldSeatIds(205L));
    }

    @Test
    @DisplayName("Redis session index should track held seat keys")
    void redisTracksSessionIndex() {
        seatLockService.holdSeats(206L, List.of(61L, 62L), "user-A", "session-index");

        Set<String> sessionKeys = redisTemplate.opsForSet().members("cinex:seat-lock:session:" + encode("session-index"));
        assertNotNull(sessionKeys);
        assertEquals(2, sessionKeys.size());
        assertTrue(sessionKeys.stream().allMatch(key -> key.startsWith("cinex:seat-lock:show:206:seat:")));
    }

    private static String encode(String value) {
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
