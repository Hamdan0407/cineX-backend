package com.bookmyshow.service;

import com.bookmyshow.dto.SeatStatusUpdateDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service managing in-memory seat locks (holds) and real-time STOMP WebSocket broadcasting.
 * Prevents duplicate seat selections during checkout and gracefully cleans up holds on disconnect.
 */
@Slf4j
@Service
public class SeatLockService {

    private static final long HOLD_TTL_MS = 10 * 60 * 1000L; // 10 minutes hold duration

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Key format: "show:{showId}:seat:{seatId}" -> SeatHoldInfo
    private final Map<String, SeatHoldInfo> activeHolds = new ConcurrentHashMap<>();
    
    // Mapping: sessionId -> Set of hold keys
    private final Map<String, Set<String>> sessionHolds = new ConcurrentHashMap<>();

    /**
     * Attempts to hold/select seats for a user. Rejects if any seat is already held by someone else.
     */
    public synchronized boolean holdSeats(Long showId, List<Long> seatIds, String userId, String sessionId) {
        long now = System.currentTimeMillis();

        // Check if any seat is currently held by a different user
        for (Long seatId : seatIds) {
            String key = buildKey(showId, seatId);
            SeatHoldInfo info = activeHolds.get(key);
            if (info != null && info.getExpiresAt() > now && !info.getUserId().equals(userId)) {
                log.warn("Seat selection rejected: Seat {} on show {} is already held by user {}", seatId, showId, info.getUserId());
                return false;
            }
        }

        // Lock seats for this user
        for (Long seatId : seatIds) {
            String key = buildKey(showId, seatId);
            SeatHoldInfo holdInfo = new SeatHoldInfo(showId, seatId, userId, sessionId, now + HOLD_TTL_MS);
            activeHolds.put(key, holdInfo);

            if (sessionId != null) {
                sessionHolds.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(key);
            }
        }

        log.info("User [{}] (session: [{}]) held seats {} for show {}", userId, sessionId, seatIds, showId);
        broadcastUpdate(showId, seatIds, "HELD", userId);
        return true;
    }

    /**
     * Releases seat holds initiated by a user or session.
     */
    public synchronized void releaseSeats(Long showId, List<Long> seatIds, String userId, String sessionId) {
        for (Long seatId : seatIds) {
            String key = buildKey(showId, seatId);
            SeatHoldInfo info = activeHolds.get(key);
            if (info != null && (userId == null || info.getUserId().equals(userId) || (sessionId != null && sessionId.equals(info.getSessionId())))) {
                activeHolds.remove(key);
                if (info.getSessionId() != null && sessionHolds.containsKey(info.getSessionId())) {
                    sessionHolds.get(info.getSessionId()).remove(key);
                }
            }
        }
        log.info("Released seats {} for show {}", seatIds, showId);
        broadcastUpdate(showId, seatIds, "AVAILABLE", userId != null ? userId : "SYSTEM");
    }

    /**
     * Called when a booking is confirmed/completed to permanently mark seats as BOOKED and release holds.
     */
    public synchronized void broadcastSeatBooked(Long showId, List<Long> seatIds, String userId) {
        for (Long seatId : seatIds) {
            String key = buildKey(showId, seatId);
            SeatHoldInfo info = activeHolds.remove(key);
            if (info != null && info.getSessionId() != null && sessionHolds.containsKey(info.getSessionId())) {
                sessionHolds.get(info.getSessionId()).remove(key);
            }
        }
        log.info("Broadcasting BOOKED status for seats {} on show {}", seatIds, showId);
        broadcastUpdate(showId, seatIds, "BOOKED", userId);
    }

    /**
     * Handles WebSocket session disconnections by releasing any seats held by the disconnecting session.
     */
    public synchronized void handleSessionDisconnect(String sessionId) {
        Set<String> keys = sessionHolds.remove(sessionId);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        log.info("Session [{}] disconnected. Cleaning up {} held seats.", sessionId, keys.size());
        Map<Long, List<Long>> showToSeatsMap = new HashMap<>();

        for (String key : keys) {
            SeatHoldInfo info = activeHolds.remove(key);
            if (info != null) {
                showToSeatsMap.computeIfAbsent(info.getShowId(), k -> new ArrayList<>()).add(info.getSeatId());
            }
        }

        showToSeatsMap.forEach((showId, seatIds) -> {
            broadcastUpdate(showId, seatIds, "AVAILABLE", "DISCONNECTED");
        });
    }

    /**
     * Returns all currently active held seat IDs for a given show.
     */
    public synchronized List<Long> getHeldSeatIds(Long showId) {
        long now = System.currentTimeMillis();
        return activeHolds.values().stream()
                .filter(info -> info.getShowId().equals(showId) && info.getExpiresAt() > now)
                .map(SeatHoldInfo::getSeatId)
                .collect(Collectors.toList());
    }

    /**
     * Scheduled background task to clean up expired seat holds every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000)
    public synchronized void cleanupExpiredHolds() {
        long now = System.currentTimeMillis();
        Map<Long, List<Long>> expiredByShow = new HashMap<>();

        Iterator<Map.Entry<String, SeatHoldInfo>> iterator = activeHolds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SeatHoldInfo> entry = iterator.next();
            SeatHoldInfo info = entry.getValue();
            if (info.getExpiresAt() <= now) {
                iterator.remove();
                if (info.getSessionId() != null && sessionHolds.containsKey(info.getSessionId())) {
                    sessionHolds.get(info.getSessionId()).remove(entry.getKey());
                }
                expiredByShow.computeIfAbsent(info.getShowId(), k -> new ArrayList<>()).add(info.getSeatId());
            }
        }

        if (!expiredByShow.isEmpty()) {
            log.info("Cleaned up expired seat holds: {}", expiredByShow);
            expiredByShow.forEach((showId, seatIds) -> broadcastUpdate(showId, seatIds, "AVAILABLE", "EXPIRED"));
        }
    }

    private void broadcastUpdate(Long showId, List<Long> seatIds, String status, String userId) {
        String destination = "/topic/shows/" + showId + "/seats";
        SeatStatusUpdateDto updateDto = SeatStatusUpdateDto.builder()
                .showId(showId)
                .seatIds(seatIds)
                .status(status)
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .build();
        try {
            messagingTemplate.convertAndSend(destination, updateDto);
        } catch (Exception e) {
            log.warn("Failed to send STOMP broadcast to destination {}: {}", destination, e.getMessage());
        }
    }

    private String buildKey(Long showId, Long seatId) {
        return "show:" + showId + ":seat:" + seatId;
    }

    @Data
    @AllArgsConstructor
    private static class SeatHoldInfo {
        private Long showId;
        private Long seatId;
        private String userId;
        private String sessionId;
        private long expiresAt;
    }
}
