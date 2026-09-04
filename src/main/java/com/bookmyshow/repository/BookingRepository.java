package com.bookmyshow.repository;

import com.bookmyshow.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByClerkUserIdOrderByBookingTimeDesc(String clerkUserId);
    List<Booking> findByClerkUserIdAndShow_IdAndBookingStatus(String clerkUserId, Long showId, String bookingStatus);
    Optional<Booking> findByTicketToken(String ticketToken);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :bookingId")
    Optional<Booking> findByIdForUpdate(Long bookingId);
    Page<Booking> findByMovieTitleContainingIgnoreCaseOrTheatreNameContainingIgnoreCaseOrClerkUserIdContainingIgnoreCase(String movieTitle, String theatreName, String clerkUserId, Pageable pageable);
}
