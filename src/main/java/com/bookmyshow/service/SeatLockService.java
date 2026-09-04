package com.bookmyshow.service;

import com.bookmyshow.dto.SeatStatusUpdateDto;
import com.bookmyshow.exception.SeatLockUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Coordinates temporary seat holds across all backend instances. */
@Slf4j
@Service
public class SeatLockService {
    private static final String LOCK_PREFIX = "cinex:seat-lock:show:";
    private static final String SESSION_PREFIX = "cinex:seat-lock:session:";

    private static final DefaultRedisScript<Long> ACQUIRE_LOCKS = new DefaultRedisScript<>("""
            for i, key in ipairs(KEYS) do
              local current = redis.call('GET', key)
              if current and string.sub(current, 1, string.len(ARGV[1])) ~= ARGV[1] then return 0 end
            end
            for i, key in ipairs(KEYS) do
              redis.call('SET', key, ARGV[2], 'PX', ARGV[3])
              redis.call('SADD', ARGV[4], key)
            end
            redis.call('PEXPIRE', ARGV[4], ARGV[3])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_LOCKS = new DefaultRedisScript<>("""
            local released = 0
            for i, key in ipairs(KEYS) do
              local current = redis.call('GET', key)
              if current then
                local matches = ARGV[1] == '*' or current == ARGV[1] or string.sub(current, 1, string.len(ARGV[1])) == ARGV[1]
                if string.sub(ARGV[1], 1, 8) == 'SESSION:' then
                  matches = string.sub(current, -string.len(ARGV[1]) + 8) == string.sub(ARGV[1], 9)
                end
                if matches then
                  redis.call('DEL', key)
                  released = released + 1
                end
              end
              if ARGV[2] ~= '' then redis.call('SREM', ARGV[2], key) end
            end
            if ARGV[2] ~= '' and redis.call('SCARD', ARGV[2]) == 0 then redis.call('DEL', ARGV[2]) end
            return released
            """, Long.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    // H2 tests select memory explicitly; all runtime profiles default to Redis.
    @Value("${cinex.seat-lock.backend:redis}")
    private String backend = "redis";

    @Value("${cinex.seat-lock.hold-ttl:10m}")
    private Duration holdTtl = Duration.ofMinutes(10);

    private final Map<String, String> testHolds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> testSessionHolds = new ConcurrentHashMap<>();

    public SeatLockService(SimpMessagingTemplate messagingTemplate, StringRedisTemplate redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    public boolean holdSeats(Long showId, List<Long> seatIds, String userId, String sessionId) {
        validateRequest(showId, seatIds, userId);
        List<Long> seats = normalizedSeatIds(seatIds);
        String prefix = ownerPrefix(userId);
        String token = prefix + encode(sessionId == null ? "REST_CLIENT" : sessionId);
        boolean locked;
        try {
            locked = usesInMemoryStore()
                    ? acquireInMemory(showId, seats, prefix, token, sessionId)
                    : acquireRedis(showId, seats, prefix, token, sessionId);
        } catch (org.springframework.data.redis.RedisConnectionFailureException ex) {
            log.warn("Seat lock unavailable for show {} seats {}: {}", showId, seats, describeRedisFailure(ex));
            throw new SeatLockUnavailableException("Seat locking is temporarily unavailable. Please try again shortly.", ex);
        }
        log.info("Seat lock request show={} seats={} ownerPresent=true result={}", showId, seats, locked);
        if (locked) broadcastUpdate(showId, seats, "HELD", userId);
        return locked;
    }

    public void releaseSeats(Long showId, List<Long> seatIds, String userId, String sessionId) {
        if (showId == null || seatIds == null || seatIds.isEmpty()) return;
        List<Long> seats = normalizedSeatIds(seatIds);
        String owner = userId == null ? "*" : ownerPrefix(userId);
        long released = usesInMemoryStore()
                ? releaseInMemory(showId, seats, owner, sessionId)
                : releaseRedis(showId, seats, owner, sessionId == null ? "" : sessionKey(sessionId));
        if (released > 0) broadcastUpdate(showId, seats, "AVAILABLE", userId == null ? "SYSTEM" : userId);
    }

    public void broadcastSeatBooked(Long showId, List<Long> seatIds, String userId) {
        if (showId == null || seatIds == null || seatIds.isEmpty()) return;
        List<Long> seats = normalizedSeatIds(seatIds);
        if (usesInMemoryStore()) releaseInMemory(showId, seats, "*", null);
        else releaseRedis(showId, seats, "*", "");
        broadcastUpdate(showId, seats, "BOOKED", userId);
    }

    public void handleSessionDisconnect(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        if (usesInMemoryStore()) { releaseInMemorySession(sessionId); return; }
        Set<String> keys = redisTemplate.opsForSet().members(sessionKey(sessionId));
        if (keys == null || keys.isEmpty()) return;
        groupKeysByShow(keys).forEach((showId, seats) -> {
            releaseRedis(showId, seats, "SESSION:" + encode(sessionId), sessionKey(sessionId));
            broadcastUpdate(showId, seats, "AVAILABLE", "DISCONNECTED");
        });
    }

    public List<Long> getHeldSeatIds(Long showId) {
        if (showId == null) return List.of();
        Set<String> keys;
        try {
            keys = usesInMemoryStore() ? testHolds.keySet() : redisTemplate.keys(lockPrefix(showId) + "*");
        } catch (org.springframework.data.redis.RedisConnectionFailureException ex) {
            log.warn("Held-seat lookup unavailable for show {}: {}", showId, describeRedisFailure(ex));
            throw new SeatLockUnavailableException("Seat availability is temporarily unavailable. Please try again shortly.", ex);
        }
        if (keys == null) return List.of();
        return keys.stream().filter(key -> key.startsWith(lockPrefix(showId)))
                .filter(key -> usesInMemoryStore() || Boolean.TRUE.equals(redisTemplate.hasKey(key)))
                .map(this::seatIdFromKey).sorted().toList();
    }

    /** Verifies that every seat remains held by the authenticated owner before payment. */
    public boolean areSeatsHeldBy(Long showId, List<Long> seatIds, String userId) {
        if (showId == null || seatIds == null || seatIds.isEmpty() || userId == null || userId.isBlank()) return false;
        String prefix = ownerPrefix(userId);
        for (Long seatId : normalizedSeatIds(seatIds)) {
            String token = usesInMemoryStore()
                    ? testHolds.get(lockKey(showId, seatId))
                    : redisTemplate.opsForValue().get(lockKey(showId, seatId));
            if (token == null || !token.startsWith(prefix)) return false;
        }
        return true;
    }

    private boolean acquireRedis(Long showId, List<Long> seats, String prefix, String token, String sessionId) {
        List<String> keys = seats.stream().map(seatId -> lockKey(showId, seatId)).toList();
        Long result = redisTemplate.execute(ACQUIRE_LOCKS, keys, prefix, token, String.valueOf(holdTtl.toMillis()),
                sessionKey(sessionId == null ? "REST_CLIENT" : sessionId));
        return Long.valueOf(1L).equals(result);
    }

    private long releaseRedis(Long showId, List<Long> seats, String owner, String sessionKey) {
        Long result = redisTemplate.execute(RELEASE_LOCKS, seats.stream().map(seatId -> lockKey(showId, seatId)).toList(), owner, sessionKey);
        return result == null ? 0 : result;
    }

    private synchronized boolean acquireInMemory(Long showId, List<Long> seats, String prefix, String token, String sessionId) {
        for (Long seatId : seats) {
            String current = testHolds.get(lockKey(showId, seatId));
            if (current != null && !current.startsWith(prefix)) return false;
        }
        String session = sessionId == null ? "REST_CLIENT" : sessionId;
        for (Long seatId : seats) {
            String key = lockKey(showId, seatId);
            testHolds.put(key, token);
            testSessionHolds.computeIfAbsent(session, ignored -> ConcurrentHashMap.newKeySet()).add(key);
        }
        return true;
    }

    private synchronized long releaseInMemory(Long showId, List<Long> seats, String owner, String sessionId) {
        long released = 0;
        for (Long seatId : seats) {
            String key = lockKey(showId, seatId);
            String current = testHolds.get(key);
            if (current != null && ("*".equals(owner) || current.startsWith(owner))) { testHolds.remove(key); released++; }
            if (sessionId != null && testSessionHolds.get(sessionId) != null) testSessionHolds.get(sessionId).remove(key);
        }
        return released;
    }

    private synchronized void releaseInMemorySession(String sessionId) {
        Set<String> keys = testSessionHolds.remove(sessionId);
        if (keys == null || keys.isEmpty()) return;
        Map<Long, List<Long>> grouped = groupKeysByShow(keys);
        keys.forEach(testHolds::remove);
        grouped.forEach((showId, seats) -> broadcastUpdate(showId, seats, "AVAILABLE", "DISCONNECTED"));
    }

    private void broadcastUpdate(Long showId, List<Long> seats, String status, String userId) {
        messagingTemplate.convertAndSend("/topic/shows/" + showId + "/seats", SeatStatusUpdateDto.builder()
                .showId(showId).seatIds(seats).status(status).userId(userId).timestamp(System.currentTimeMillis()).build());
    }

    private boolean usesInMemoryStore() { return "memory".equalsIgnoreCase(backend); }

    /**
     * Renders a Redis failure as exception type plus root-cause detail. The previous fixed
     * "Redis connection failed" string hid the actual fault: a host-side port-forward that stops
     * listening reports "Connection refused", which is indistinguishable from auth or timeout
     * problems until the root cause is printed. Contains no credentials or tokens.
     */
    private String describeRedisFailure(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return ex.getClass().getSimpleName() + " (redis host=" + redisEndpointDescription() + ") root="
                + root.getClass().getName() + ": " + root.getMessage();
    }

    /** Best-effort endpoint description for diagnostics; never includes the Redis password. */
    private String redisEndpointDescription() {
        try {
            org.springframework.data.redis.connection.RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory lettuce) {
                return lettuce.getHostName() + ":" + lettuce.getPort();
            }
        } catch (RuntimeException ignored) { /* diagnostics must never mask the original failure */ }
        return "unknown";
    }

    private void validateRequest(Long showId, List<Long> seats, String userId) {
        if (showId == null || seats == null || seats.isEmpty() || userId == null || userId.isBlank())
            throw new IllegalArgumentException("showId, seatIds, and userId are required to hold seats");
    }
    private List<Long> normalizedSeatIds(Collection<Long> seats) { return seats.stream().filter(java.util.Objects::nonNull).distinct().sorted().toList(); }
    private String lockKey(Long showId, Long seatId) { return lockPrefix(showId) + seatId; }
    private String lockPrefix(Long showId) { return LOCK_PREFIX + showId + ":seat:"; }
    private String sessionKey(String sessionId) { return SESSION_PREFIX + encode(sessionId); }
    private String ownerPrefix(String userId) { return encode(userId) + "."; }
    private String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private Long seatIdFromKey(String key) { return Long.valueOf(key.substring(key.lastIndexOf(':') + 1)); }
    private Map<Long, List<Long>> groupKeysByShow(Collection<String> keys) {
        Map<Long, List<Long>> result = new HashMap<>();
        for (String key : keys) {
            String[] parts = key.split(":");
            if (parts.length >= 6) result.computeIfAbsent(Long.valueOf(parts[3]), ignored -> new ArrayList<>()).add(Long.valueOf(parts[5]));
        }
        return result;
    }
}
