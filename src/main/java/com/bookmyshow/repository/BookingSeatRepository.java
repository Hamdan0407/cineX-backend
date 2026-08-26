package com.bookmyshow.repository;

import com.bookmyshow.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByBookingShowId(Long showId);
    List<BookingSeat> findByBookingId(Long bookingId);

    @Query("SELECT count(bs) > 0 FROM BookingSeat bs WHERE bs.seat.id = :seatId AND bs.booking.show.id = :showId AND bs.status IN ('BOOKED', 'PENDING')")
    boolean isSeatBooked(@Param("seatId") Long seatId, @Param("showId") Long showId);
}
