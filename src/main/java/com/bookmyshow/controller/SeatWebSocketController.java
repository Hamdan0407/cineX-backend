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

    /**
     * STOMP message handler for seat selection/deselection over WebSocket.
     * Destination: /app/shows/select-seat
     */
    @MessageMapping("/shows/select-seat")
    public void handleSeatSelection(@Payload SeatSelectRequestDto request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor != null ? headerAccessor.getSessionId() : "REST_OR_TEST";
        log.info("Received STOMP seat action [{}] from user [{}] (session: [{}]) for show {} seats {}",
                request.getAction(), request.getUserId(), sessionId, request.getShowId(), request.getSeatIds());

        if ("SELECT".equalsIgnoreCase(request.getAction())) {
            seatLockService.holdSeats(request.getShowId(), request.getSeatIds(), request.getUserId(), sessionId);
        } else if ("DESELECT".equalsIgnoreCase(request.getAction())) {
            seatLockService.releaseSeats(request.getShowId(), request.getSeatIds(), request.getUserId(), sessionId);
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
    public ResponseEntity<Boolean> lockSeatsRest(@PathVariable Long showId, @RequestBody SeatSelectRequestDto request) {
        boolean success = seatLockService.holdSeats(showId, request.getSeatIds(), request.getUserId(), "REST_CLIENT");
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
    public ResponseEntity<Void> unlockSeatsRest(@PathVariable Long showId, @RequestBody SeatSelectRequestDto request) {
        seatLockService.releaseSeats(showId, request.getSeatIds(), request.getUserId(), null);
        return ResponseEntity.ok().build();
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
