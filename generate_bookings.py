import os

base_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/src/main/java/com/bookmyshow'

files = {
    'dto/BookingRequest.java': '''package com.bookmyshow.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    private Long userId;
    private Long showId;
    private List<Long> seatIds;
}
''',
    'dto/BookingResponse.java': '''package com.bookmyshow.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Long bookingId;
    private Long userId;
    private Long showId;
    private String status;
    private Double totalAmount;
    private LocalDateTime bookingTime;
    private List<String> seatNumbers;
}
''',
    'repository/BookingRepository.java': '''package com.bookmyshow.repository;

import com.bookmyshow.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
}
''',
    'repository/BookingSeatRepository.java': '''package com.bookmyshow.repository;

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
''',
    'service/BookingService.java': '''package com.bookmyshow.service;

import com.bookmyshow.dto.BookingRequest;
import com.bookmyshow.dto.BookingResponse;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.entity.BookingSeat;
import com.bookmyshow.entity.Seat;
import com.bookmyshow.entity.Show;
import com.bookmyshow.entity.User;
import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.repository.BookingSeatRepository;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.ShowRepository;
import com.bookmyshow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public BookingService(BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
                          UserRepository userRepository, ShowRepository showRepository, SeatRepository seatRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.userRepository = userRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new RuntimeException("No seats selected");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new RuntimeException("Show not found"));

        List<Seat> seatsToBook = new ArrayList<>();
        for (Long seatId : request.getSeatIds()) {
            if (bookingSeatRepository.isSeatBooked(seatId, show.getId())) {
                throw new RuntimeException("Seat already booked");
            }
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
            seatsToBook.add(seat);
        }

        Double totalAmount = seatsToBook.size() * show.getPrice();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setTotalAmount(totalAmount);
        booking.setStatus("PENDING");
        booking.setBookingTime(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (Seat seat : seatsToBook) {
            BookingSeat bs = new BookingSeat();
            bs.setBooking(booking);
            bs.setSeat(seat);
            bs.setStatus("PENDING");
            bookingSeats.add(bs);
        }
        bookingSeatRepository.saveAll(bookingSeats);

        // Mock payment process -> 80% chance to succeed
        boolean paymentSuccess = Math.random() > 0.2;
        
        if (paymentSuccess) {
            booking.setStatus("CONFIRMED");
            bookingSeats.forEach(bs -> bs.setStatus("BOOKED"));
        } else {
            booking.setStatus("FAILED");
            bookingSeats.forEach(bs -> bs.setStatus("FAILED")); // Released back implicitly
        }

        bookingRepository.save(booking);
        bookingSeatRepository.saveAll(bookingSeats);

        return mapToDto(booking, seatsToBook);
    }

    public BookingResponse getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        List<Seat> seats = bookingSeatRepository.findByBookingId(bookingId).stream()
                .map(BookingSeat::getSeat).collect(Collectors.toList());
        return mapToDto(booking, seats);
    }

    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId).stream().map(booking -> {
            List<Seat> seats = bookingSeatRepository.findByBookingId(booking.getId()).stream()
                    .map(BookingSeat::getSeat).collect(Collectors.toList());
            return mapToDto(booking, seats);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new RuntimeException("Only CONFIRMED bookings can be cancelled");
        }
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        bookingSeats.forEach(bs -> bs.setStatus("CANCELLED"));
        bookingSeatRepository.saveAll(bookingSeats);
    }

    private BookingResponse mapToDto(Booking booking, List<Seat> seats) {
        BookingResponse dto = new BookingResponse();
        dto.setBookingId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setShowId(booking.getShow().getId());
        dto.setStatus(booking.getStatus());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setBookingTime(booking.getBookingTime());
        dto.setSeatNumbers(seats.stream().map(Seat::getSeatNumber).collect(Collectors.toList()));
        return dto;
    }
}
''',
    'controller/BookingController.java': '''package com.bookmyshow.controller;

import com.bookmyshow.dto.BookingRequest;
import com.bookmyshow.dto.BookingResponse;
import com.bookmyshow.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        try {
            BookingResponse response = bookingService.createBooking(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBooking(@PathVariable Long bookingId) {
        try {
            return ResponseEntity.ok(bookingService.getBooking(bookingId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {
        try {
            bookingService.cancelBooking(bookingId);
            return ResponseEntity.ok("Booking cancelled successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
'''
}

for path, content in files.items():
    full_path = os.path.join(base_path, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w') as f:
        f.write(content)

print("All booking files generated successfully.")