package com.bookmyshow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Broadcast DTO representing real-time seat state changes sent over WebSocket to all clients viewing a show.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusUpdateDto {
    private Long showId;
    private List<Long> seatIds;
    private String status; // HELD, AVAILABLE, BOOKED
    private String userId; // ID of user holding/booking the seat
    private Long timestamp;
}
