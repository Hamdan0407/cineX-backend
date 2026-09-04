package com.bookmyshow.service;

import com.bookmyshow.dto.SeatStatusUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SeatLockServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private SeatLockService seatLockService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(seatLockService, "backend", "memory");
    }

    @Test
    @DisplayName("Should hold available seats and broadcast HELD status")
    void testHoldSeatsSuccess() {
        boolean result = seatLockService.holdSeats(101L, List.of(1L, 2L), "user-A", "session-1");
        assertTrue(result, "Holding available seats should succeed");

        List<Long> held = seatLockService.getHeldSeatIds(101L);
        assertEquals(2, held.size());
        assertTrue(held.containsAll(List.of(1L, 2L)));

        ArgumentCaptor<SeatStatusUpdateDto> captor = ArgumentCaptor.forClass(SeatStatusUpdateDto.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/shows/101/seats"), captor.capture());
        assertEquals("HELD", captor.getValue().getStatus());
        assertEquals("user-A", captor.getValue().getUserId());
    }

    @Test
    @DisplayName("Should prevent duplicate seat selection by another user")
    void testPreventDuplicateSeatSelection() {
        seatLockService.holdSeats(101L, List.of(1L, 2L), "user-A", "session-1");

        // user-B tries to hold overlapping seat 2L
        boolean result = seatLockService.holdSeats(101L, List.of(2L, 3L), "user-B", "session-2");
        assertFalse(result, "Should reject seat lock if seat is already held by another user");
    }

    @Test
    @DisplayName("Should release seats and broadcast AVAILABLE status")
    void testReleaseSeats() {
        seatLockService.holdSeats(101L, List.of(1L, 2L), "user-A", "session-1");
        seatLockService.releaseSeats(101L, List.of(1L, 2L), "user-A", "session-1");

        List<Long> held = seatLockService.getHeldSeatIds(101L);
        assertTrue(held.isEmpty(), "No seats should be held after release");

        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/shows/101/seats"), any(SeatStatusUpdateDto.class));
    }

    @Test
    @DisplayName("Should gracefully release seats on session disconnect")
    void testHandleSessionDisconnect() {
        seatLockService.holdSeats(101L, List.of(10L, 11L), "user-A", "session-disconnect");
        assertEquals(2, seatLockService.getHeldSeatIds(101L).size());

        seatLockService.handleSessionDisconnect("session-disconnect");
        assertTrue(seatLockService.getHeldSeatIds(101L).isEmpty(), "Seats should be released upon disconnect");
    }

    @Test
    @DisplayName("Should reject release attempts from another user")
    void testPreventReleaseByAnotherUser() {
        seatLockService.holdSeats(101L, List.of(1L), "user-A", "session-1");

        seatLockService.releaseSeats(101L, List.of(1L), "user-B", "session-2");

        List<Long> held = seatLockService.getHeldSeatIds(101L);
        assertEquals(1, held.size(), "Another user must not be able to release held seats");
        assertTrue(held.contains(1L));
    }

    @Test
    @DisplayName("Should allow same user to extend hold on already-held seats")
    void testSameUserCanRefreshHold() {
        assertTrue(seatLockService.holdSeats(101L, List.of(4L), "user-A", "session-1"));
        assertTrue(seatLockService.holdSeats(101L, List.of(4L, 5L), "user-A", "session-1"));

        List<Long> held = seatLockService.getHeldSeatIds(101L);
        assertEquals(2, held.size());
        assertTrue(held.containsAll(List.of(4L, 5L)));
    }

    @Test
    @DisplayName("Should broadcast BOOKED status when booking completes")
    void testBroadcastSeatBooked() {
        seatLockService.holdSeats(101L, List.of(5L), "user-A", "session-1");
        seatLockService.broadcastSeatBooked(101L, List.of(5L), "user-A");

        assertTrue(seatLockService.getHeldSeatIds(101L).isEmpty(), "Holds should be cleared once booked");

        ArgumentCaptor<SeatStatusUpdateDto> captor = ArgumentCaptor.forClass(SeatStatusUpdateDto.class);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/shows/101/seats"), captor.capture());
        assertEquals("BOOKED", captor.getAllValues().get(1).getStatus());
    }
}
