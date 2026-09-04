package com.bookmyshow.controller;

import com.bookmyshow.dto.SeatSelectRequestDto;
import com.bookmyshow.service.SeatLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * Controller handling real-time seat selection and lock broadcasting via WebSocket STOMP and REST APIs.
 */
@Slf4j
@Controller
@Tag(name = "Live Seat Updates", description = "WebSocket STOMP & REST APIs for real-time interactive seat locking")
public class SeatWebSocketController {

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private com.bookmyshow.service.AdminAuthService adminAuthService;

    @Value("${cinex.security.allow-test-auth-bypass:false}")
    private boolean allowTestAuthBypass;

    /**
     * STOMP message handler for seat selection/deselection over WebSocket.
     * Destination: /app/shows/select-seat
     */
    @MessageMapping("/shows/select-seat")
    public void handleSeatSelection(@Payload SeatSelectRequestDto request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor != null ? headerAccessor.getSessionId() : "REST_OR_TEST";
        log.info("Received STOMP seat action [{}] from user [{}] (session: [{}]) for show {} seats {}",
                request.getAction(), request.getUserId(), sessionId, request.getShowId(), request.getSeatIds());

        String lockUserId = resolveLockUserId(request);
        if ("SELECT".equalsIgnoreCase(request.getAction())) {
            boolean locked = seatLockService.holdSeats(request.getShowId(), request.getSeatIds(), lockUserId, sessionId);
            log.info("STOMP seat lock completed show={} seats={} principalPresent=true result={}",
                    request.getShowId(), request.getSeatIds(), locked);
        } else if ("DESELECT".equalsIgnoreCase(request.getAction())) {
            seatLockService.releaseSeats(request.getShowId(), request.getSeatIds(), lockUserId, sessionId);
        } else {
            log.warn("Unknown seat selection action: {}", request.getAction());
        }
    }

    /**
     * REST endpoint to lock seats (useful for fallback or REST-based initiation).
     */
    @ResponseBody
    @PostMapping("/api/shows/{showId}/seats/lock")
    @Operation(summary = "Lock seats temporarily via REST API")
    public ResponseEntity<Boolean> lockSeatsRest(@PathVariable Long showId, @RequestBody SeatSelectRequestDto request,
                                                  @RequestHeader(value = "X-Seat-Lock-Session", required = false) String sessionId) {
        String lockUserId = resolveLockUserId(request);
        boolean success = seatLockService.holdSeats(showId, request.getSeatIds(), lockUserId, sessionId);
        log.info("REST seat lock completed show={} seats={} principalPresent=true result={}", showId, request.getSeatIds(), success);
        if (success) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.status(409).body(false);
        }
    }

    /**
     * REST endpoint to release locked seats.
     */
    @ResponseBody
    @DeleteMapping("/api/shows/{showId}/seats/lock")
    @Operation(summary = "Release temporarily locked seats via REST API")
    public ResponseEntity<Void> unlockSeatsRest(@PathVariable Long showId, @RequestBody SeatSelectRequestDto request,
                                                 @RequestHeader(value = "X-Seat-Lock-Session", required = false) String sessionId) {
        seatLockService.releaseSeats(showId, request.getSeatIds(), resolveLockUserId(request), sessionId);
        return ResponseEntity.ok().build();
    }

    private String resolveLockUserId(SeatSelectRequestDto request) {
        try {
            return adminAuthService.getAuthenticatedClerkUserId();
        } catch (SecurityException ex) {
            // Only H2/controller tests may exercise the legacy no-token path. Runtime lock ownership
            // always comes from the verified Clerk principal, never from the JSON request body.
            if (allowTestAuthBypass && request.getUserId() != null && !request.getUserId().isBlank()) {
                return request.getUserId();
            }
            throw ex;
        }
    }

    /**
     * REST endpoint to fetch currently held seat IDs for initial page load.
     */
    @ResponseBody
    @GetMapping("/api/shows/{showId}/seats/held")
    @Operation(summary = "Get list of seat IDs currently held by users for a show")
    public ResponseEntity<List<Long>> getHeldSeats(@PathVariable Long showId) {
        return ResponseEntity.ok(seatLockService.getHeldSeatIds(showId));
    }
}
