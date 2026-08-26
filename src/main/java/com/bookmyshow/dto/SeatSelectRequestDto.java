package com.bookmyshow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload DTO sent by clients over STOMP / WebSocket when selecting or deselecting seats on the interactive map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatSelectRequestDto {
    private Long showId;
    private List<Long> seatIds;
    private String action; // SELECT, DESELECT
    private String userId;
}
