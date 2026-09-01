package com.bookmyshow.service;

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
import com.bookmyshow.dto.PaymentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bookmyshow.exception.SeatAlreadyBookedException;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.exception.ValidationException;
import com.bookmyshow.exception.PaymentFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SeatLockService seatLockService;

    public BookingService(BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
                          UserRepository userRepository, ShowRepository showRepository, SeatRepository seatRepository,
                          PaymentService paymentService, EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.userRepository = userRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
    }

    @Transactional
    @CacheEvict(value = "shows", allEntries = true)
    public BookingResponse createBooking(BookingRequest request) {
        if (request.getShowId() == null) {
            throw new ValidationException("showId is required");
        }
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new ValidationException("At least one seatId is required");
        }

        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));
        }

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + request.getShowId()));

        List<Seat> seatsToBook = new ArrayList<>();
        Double totalAmount = request.getAmount() != null ? request.getAmount() : 0.0;

        // Sort IDs before acquiring pessimistic locks to prevent transaction deadlocks.
        List<Long> sortedSeatIds = request.getSeatIds().stream()
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        for (Long seatId : sortedSeatIds) {
            Seat seat = seatRepository.findByIdWithLock(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seatId));

            if (seat.getScreen() == null || show.getScreen() == null
                    || !seat.getScreen().getId().equals(show.getScreen().getId())) {
                throw new ValidationException("Seat " + seatId + " does not belong to the screen for show " + show.getId());
            }

            if (bookingSeatRepository.isSeatBooked(seatId, show.getId())) {
                throw new SeatAlreadyBookedException("Seat already booked for show " + show.getId());
            }
            seatsToBook.add(seat);
        }
        if (request.getAmount() == null) {
            totalAmount = seatsToBook.size() * show.getPrice();
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setClerkUserId(request.getClerkUserId());
        booking.setShow(show);
        booking.setMovieId(request.getMovieId() != null ? request.getMovieId() : (show != null && show.getMovie() != null ? show.getMovie().getId() : null));
        booking.setTheatreId(request.getTheatreId() != null ? request.getTheatreId() : (show != null && show.getScreen() != null && show.getScreen().getTheatre() != null ? show.getScreen().getTheatre().getId() : null));
        
        String seatsStr = seatsToBook.stream().map(Seat::getSeatNumber).collect(Collectors.joining(","));
        booking.setSeatIds(seatsStr);
        booking.setAmount(totalAmount);
        booking.setTotalAmount(totalAmount);
        
        booking.setMovieTitle(request.getMovieTitle() != null ? request.getMovieTitle() : (show != null && show.getMovie() != null ? show.getMovie().getTitle() : "Movie"));
        booking.setPosterPath(request.getPosterPath() != null ? request.getPosterPath() : (show != null && show.getMovie() != null ? show.getMovie().getPosterPath() : ""));
        booking.setTheatreName(request.getTheatreName() != null ? request.getTheatreName() : (show != null && show.getScreen() != null && show.getScreen().getTheatre() != null ? show.getScreen().getTheatre().getName() : "CineX Theatre"));
        booking.setCityName(request.getCityName() != null ? request.getCityName() : "Mumbai");
        booking.setShowDate(request.getShowDate() != null ? request.getShowDate() : (show != null && show.getShowDate() != null ? show.getShowDate().toString() : "Today"));
        booking.setShowTime(request.getShowTime() != null ? request.getShowTime() : (show != null && show.getShowTime() != null ? show.getShowTime().toString() : "Now"));
        booking.setUserEmail(request.getUserEmail() != null ? request.getUserEmail() : (user != null ? user.getEmail() : null));
        
        booking.setStatus("PENDING");
        booking.setBookingStatus("PENDING_PAYMENT");
        booking.setPaymentStatus("PENDING");
        booking.setBookingTime(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = new ArrayList<>();
        if (!seatsToBook.isEmpty()) {
            for (Seat seat : seatsToBook) {
                BookingSeat bs = new BookingSeat();
                bs.setBooking(booking);
                bs.setSeat(seat);
                bs.setStatus("PENDING");
                bookingSeats.add(bs);
            }
            bookingSeatRepository.saveAll(bookingSeats);
        }

        return mapToDto(booking, seatsToBook);
    }

    public BookingResponse getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
        return mapToDto(booking, null);
    }

    public List<BookingResponse> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream().map(b -> mapToDto(b, null)).collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByClerkUserId(String clerkUserId) {
        List<Booking> bookings = bookingRepository.findByClerkUserIdOrderByBookingTimeDesc(clerkUserId);
        return bookings.stream().map(b -> mapToDto(b, null)).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "shows", allEntries = true)
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!"CONFIRMED".equals(booking.getStatus()) && !"BOOKED".equals(booking.getBookingStatus())) {
            throw new ValidationException("Only CONFIRMED or BOOKED bookings can be cancelled");
        }
        booking.setStatus("CANCELLED");
        booking.setBookingStatus("CANCELLED");
        bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        if (bookingSeats != null && !bookingSeats.isEmpty()) {
            bookingSeats.forEach(bs -> bs.setStatus("CANCELLED"));
            bookingSeatRepository.saveAll(bookingSeats);
            if (seatLockService != null && booking.getShow() != null) {
                List<Long> sIds = bookingSeats.stream().map(bs -> bs.getSeat().getId()).collect(Collectors.toList());
                seatLockService.releaseSeats(booking.getShow().getId(), sIds, null, null);
            }
        }
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .sorted((b1, b2) -> {
                    if (b1.getBookingTime() != null && b2.getBookingTime() != null) {
                        return b2.getBookingTime().compareTo(b1.getBookingTime());
                    }
                    return b2.getId().compareTo(b1.getId());
                })
                .map(b -> mapToDto(b, null)).collect(Collectors.toList());
    }

    public Page<BookingResponse> getBookingsPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookingRepository.findAll(pageable).map(b -> mapToDto(b, null));
    }

    public Page<BookingResponse> searchBookingsPaginated(String query, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookingRepository.findByMovieTitleContainingIgnoreCaseOrTheatreNameContainingIgnoreCaseOrClerkUserIdContainingIgnoreCase(query, query, query, pageable)
                .map(b -> mapToDto(b, null));
    }

    public List<BookingResponse> searchBookings(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllBookings();
        }
        String q = query.trim().toLowerCase();
        return bookingRepository.findAll().stream()
                .filter(b -> (b.getId() != null && String.valueOf(b.getId()).contains(q)) ||
                             (b.getClerkUserId() != null && b.getClerkUserId().toLowerCase().contains(q)) ||
                             (b.getTicketToken() != null && b.getTicketToken().toLowerCase().contains(q)) ||
                             (b.getMovieTitle() != null && b.getMovieTitle().toLowerCase().contains(q)) ||
                             (b.getTheatreName() != null && b.getTheatreName().toLowerCase().contains(q)) ||
                             (b.getCityName() != null && b.getCityName().toLowerCase().contains(q)) ||
                             (b.getStatus() != null && b.getStatus().toLowerCase().contains(q)) ||
                             (b.getPaymentStatus() != null && b.getPaymentStatus().toLowerCase().contains(q)))
                .map(b -> mapToDto(b, null))
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "shows", allEntries = true)
    public BookingResponse updateBookingStatus(Long bookingId, String bookingStatus, String paymentStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (bookingStatus != null && !bookingStatus.trim().isEmpty()) {
            booking.setStatus(bookingStatus);
            booking.setBookingStatus(bookingStatus);
            if ("CANCELLED".equalsIgnoreCase(bookingStatus) || "REFUNDED".equalsIgnoreCase(bookingStatus)) {
                List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
                if (bookingSeats != null) {
                    bookingSeats.forEach(bs -> bs.setStatus("CANCELLED"));
                    bookingSeatRepository.saveAll(bookingSeats);
                }
            }
        }
        if (paymentStatus != null && !paymentStatus.trim().isEmpty()) {
            booking.setPaymentStatus(paymentStatus);
        }
        Booking updated = bookingRepository.save(booking);
        return mapToDto(updated, null);
    }

    private BookingResponse mapToDto(Booking booking, List<Seat> seats) {
        BookingResponse dto = new BookingResponse();
        dto.setBookingId(booking.getId());
        dto.setClerkUserId(booking.getClerkUserId());
        dto.setUserId(booking.getUser() != null ? booking.getUser().getId() : null);
        if (booking.getShow() != null) {
            dto.setShowId(booking.getShow().getId());
            if (booking.getShow().getMovie() != null) {
                dto.setMovieId(booking.getShow().getMovie().getId());
                dto.setMovieTitle(booking.getShow().getMovie().getTitle());
                dto.setPosterPath(booking.getShow().getMovie().getPosterPath());
            }
            if (booking.getShow().getScreen() != null) {
                dto.setScreenName(booking.getShow().getScreen().getScreenName());
                if (booking.getShow().getScreen().getTheatre() != null) {
                    dto.setTheatreId(booking.getShow().getScreen().getTheatre().getId());
                    dto.setTheatreName(booking.getShow().getScreen().getTheatre().getName());
                    dto.setCityName(booking.getShow().getScreen().getTheatre().getCity());
                }
            }
            dto.setShowDate(booking.getShow().getShowDate() != null ? booking.getShow().getShowDate().toString() : "");
            dto.setShowTime(booking.getShow().getShowTime() != null ? booking.getShow().getShowTime().toString() : "");
        } else {
            dto.setMovieId(booking.getMovieId());
            dto.setTheatreId(booking.getTheatreId());
            dto.setMovieTitle(booking.getMovieTitle());
            dto.setPosterPath(booking.getPosterPath());
            dto.setTheatreName(booking.getTheatreName());
            dto.setCityName(booking.getCityName());
            dto.setShowDate(booking.getShowDate());
            dto.setShowTime(booking.getShowTime());
        }
        dto.setSeatIds(booking.getSeatIds());
        dto.setAmount(booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount());
        dto.setTotalAmount(dto.getAmount());
        dto.setBookingStatus(booking.getBookingStatus() != null ? booking.getBookingStatus() : booking.getStatus());
        dto.setStatus(dto.getBookingStatus());
        dto.setPaymentStatus(booking.getPaymentStatus());
        dto.setPaymentId(booking.getPaymentId());
        dto.setBookingTime(booking.getBookingTime());
        
        List<String> seatNums = new ArrayList<>();
        if (seats != null && !seats.isEmpty()) {
            seatNums = seats.stream().map(Seat::getSeatNumber).collect(Collectors.toList());
        } else if (booking.getSeatIds() != null && !booking.getSeatIds().isEmpty()) {
            for (String s : booking.getSeatIds().split(",")) {
                if (!s.trim().isEmpty()) {
                    seatNums.add(s.trim());
                }
            }
        }
        dto.setSeatNumbers(seatNums);

        if (booking.getTicketToken() == null && ("BOOKED".equals(booking.getBookingStatus()) || "CONFIRMED".equals(booking.getStatus()))) {
            booking.setTicketToken(java.util.UUID.randomUUID().toString());
            bookingRepository.save(booking);
        }
        dto.setTicketToken(booking.getTicketToken());

        // Populate frontend aliases
        dto.setId("CNX-" + booking.getId());
        dto.setMovie(dto.getMovieTitle());
        dto.setPoster(dto.getPosterPath());
        dto.setTheatre(dto.getTheatreName());
        dto.setDate(dto.getShowDate());
        dto.setTime(dto.getShowTime());
        dto.setSeats(seatNums);
        dto.setTotal(dto.getAmount());

        return dto;
    }
}
