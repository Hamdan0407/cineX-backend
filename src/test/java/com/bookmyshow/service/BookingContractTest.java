package com.bookmyshow.service;

import com.bookmyshow.dto.BookingRequest;
import com.bookmyshow.dto.BookingResponse;
import com.bookmyshow.entity.BookingSeat;
import com.bookmyshow.entity.Movie;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Seat;
import com.bookmyshow.entity.Show;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.exception.SeatAlreadyBookedException;
import com.bookmyshow.exception.ValidationException;
import com.bookmyshow.repository.BookingSeatRepository;
import com.bookmyshow.repository.MovieRepository;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.ShowRepository;
import com.bookmyshow.repository.TheatreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BookingContractTest {

    @Autowired private BookingService bookingService;
    @Autowired private MovieRepository movieRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;

    @MockitoBean
    private PaymentService paymentService;

    private Show targetShow;
    private Seat targetSeat;
    private Seat otherScreenSeat;

    @BeforeEach
    void setUp() {
        Movie movie = new Movie();
        movie.setTitle("Contract Test Movie");
        movie = movieRepository.save(movie);

        Theatre theatre = new Theatre();
        theatre.setName("Contract Test Theatre");
        theatre.setCity("Test City");
        theatre = theatreRepository.save(theatre);

        Screen targetScreen = new Screen();
        targetScreen.setScreenName("Target Screen");
        targetScreen.setTheatre(theatre);
        targetScreen = screenRepository.save(targetScreen);

        Screen otherScreen = new Screen();
        otherScreen.setScreenName("Other Screen");
        otherScreen.setTheatre(theatre);
        otherScreen = screenRepository.save(otherScreen);

        targetSeat = seat("A1", targetScreen);
        otherScreenSeat = seat("A1", otherScreen);

        targetShow = new Show();
        targetShow.setMovie(movie);
        targetShow.setScreen(targetScreen);
        targetShow.setShowDate(LocalDate.now());
        targetShow.setShowTime(LocalTime.of(12, 0));
        targetShow.setPrice(250.0);
        targetShow = showRepository.save(targetShow);
    }

    private Seat seat(String number, Screen screen) {
        Seat seat = new Seat();
        seat.setSeatNumber(number);
        seat.setSeatType("PREMIUM");
        seat.setPrice(250.0);
        seat.setScreen(screen);
        return seatRepository.save(seat);
    }

    private BookingRequest request(Long showId, List<Long> seatIds) {
        BookingRequest request = new BookingRequest();
        request.setShowId(showId);
        request.setSeatIds(seatIds);
        request.setClerkUserId("contract-test-user");
        return request;
    }

    @Test
    void validShowAndSeatsCreateBookingRelationships() {
        BookingResponse response = bookingService.createBooking(request(targetShow.getId(), List.of(targetSeat.getId())));

        assertEquals(targetShow.getId(), response.getShowId());
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(response.getBookingId());
        assertEquals(1, bookingSeats.size());
        assertEquals(targetSeat.getId(), bookingSeats.get(0).getSeat().getId());
        assertEquals(targetShow.getId(), bookingSeats.get(0).getBooking().getShow().getId());
    }

    @Test
    void nonExistentShowIsRejected() {
        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(request(Long.MAX_VALUE, List.of(targetSeat.getId()))));
    }

    @Test
    void nonExistentSeatIsRejected() {
        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(request(targetShow.getId(), List.of(Long.MAX_VALUE))));
    }

    @Test
    void seatFromAnotherScreenIsRejected() {
        assertThrows(ValidationException.class,
                () -> bookingService.createBooking(request(targetShow.getId(), List.of(otherScreenSeat.getId()))));
    }

    @Test
    void mixedValidAndForeignSeatsRejectEntireBooking() {
        assertThrows(ValidationException.class,
                () -> bookingService.createBooking(request(targetShow.getId(),
                        List.of(targetSeat.getId(), otherScreenSeat.getId()))));
    }

    @Test
    void alreadyBookedSeatIsRejected() {
        BookingResponse firstBooking = bookingService.createBooking(request(targetShow.getId(), List.of(targetSeat.getId())));
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(firstBooking.getBookingId());
        bookingSeats.forEach(bookingSeat -> bookingSeat.setStatus("BOOKED"));
        bookingSeatRepository.saveAll(bookingSeats);

        assertThrows(SeatAlreadyBookedException.class,
                () -> bookingService.createBooking(request(targetShow.getId(), List.of(targetSeat.getId()))));
    }
}
