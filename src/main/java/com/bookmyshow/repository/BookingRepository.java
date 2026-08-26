package com.bookmyshow.repository;

import com.bookmyshow.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByClerkUserIdOrderByBookingTimeDesc(String clerkUserId);
    Optional<Booking> findByTicketToken(String ticketToken);
    Page<Booking> findByMovieTitleContainingIgnoreCaseOrTheatreNameContainingIgnoreCaseOrClerkUserIdContainingIgnoreCase(String movieTitle, String theatreName, String clerkUserId, Pageable pageable);
}
