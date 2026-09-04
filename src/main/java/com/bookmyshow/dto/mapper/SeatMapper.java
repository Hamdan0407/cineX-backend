package com.bookmyshow.dto.mapper;

import com.bookmyshow.dto.SeatRequest;
import com.bookmyshow.dto.SeatResponse;
import com.bookmyshow.entity.Seat;

public final class SeatMapper {

    private SeatMapper() {
    }

    public static void applyRequest(Seat seat, SeatRequest request) {
        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatType(request.getSeatType());
        seat.setPrice(request.getPrice());
    }

    public static SeatResponse toResponse(Seat seat) {
        SeatResponse response = new SeatResponse();
        response.setId(seat.getId());
        response.setSeatNumber(seat.getSeatNumber());
        response.setSeatType(seat.getSeatType());
        response.setPrice(seat.getPrice());
        response.setScreenId(seat.getScreen().getId());
        return response;
    }

    public static SeatRequest toRequest(SeatResponse source) {
        if (source == null) {
            return null;
        }
        SeatRequest request = new SeatRequest();
        request.setSeatNumber(source.getSeatNumber());
        request.setSeatType(source.getSeatType());
        request.setPrice(source.getPrice());
        request.setScreenId(source.getScreenId());
        return request;
    }
}
