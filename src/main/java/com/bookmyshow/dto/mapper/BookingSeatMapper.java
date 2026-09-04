package com.bookmyshow.dto.mapper;

import com.bookmyshow.dto.BookingSeatResponse;
import com.bookmyshow.entity.BookingSeat;

public final class BookingSeatMapper {

    private BookingSeatMapper() {
    }

    public static BookingSeatResponse toResponse(BookingSeat bookingSeat) {
        BookingSeatResponse response = new BookingSeatResponse();
        response.setId(bookingSeat.getId());
        response.setBookingId(bookingSeat.getBooking().getId());
        response.setSeatId(bookingSeat.getSeat().getId());
        response.setSeatNumber(bookingSeat.getSeat().getSeatNumber());
        response.setStatus(bookingSeat.getStatus());
        return response;
    }
}
