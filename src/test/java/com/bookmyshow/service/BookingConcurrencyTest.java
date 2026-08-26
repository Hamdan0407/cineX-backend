package com.bookmyshow.service;

import com.bookmyshow.dto.BookingRequest;
import com.bookmyshow.dto.PaymentResponse;
import com.bookmyshow.entity.Seat;
import com.bookmyshow.entity.Show;
import com.bookmyshow.entity.User;
import com.bookmyshow.exception.SeatAlreadyBookedException;
import com.bookmyshow.repository.BookingSeatRepository;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.ShowRepository;
import com.bookmyshow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.PessimisticLockingFailureException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
public class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("Multithreaded concurrency test: exactly one thread should succeed when 10 threads attempt to book the same seat simultaneously")
    public void testConcurrentBooking_PreventsDoubleBooking() throws InterruptedException {
        // 1. Ensure a valid test user exists
        User user = new User();
        user.setName("Concurrency Tester");
        user.setEmail("tester@cinex.com");
        user.setPassword("password123");
        user = userRepository.save(user);

        // 2. Fetch an existing show and one of its seats
        List<Show> shows = showRepository.findAll();
        assertFalse(shows.isEmpty(), "Sample shows must be initialized in the database");
        Show targetShow = shows.get(0);

        List<Seat> screenSeats = seatRepository.findByScreenId(targetShow.getScreen().getId());
        assertFalse(screenSeats.isEmpty(), "Screen must have seats initialized");
        Seat targetSeat = screenSeats.get(0);


        // 4. Prepare concurrent booking request for the exact same seat
        BookingRequest request = new BookingRequest();
        request.setUserId(user.getId());
        request.setShowId(targetShow.getId());
        request.setSeatIds(List.of(targetSeat.getId()));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 5. Submit 10 concurrent worker threads
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Hold threads until the start signal
                    bookingService.createBooking(request);
                    successCount.incrementAndGet();
                } catch (SeatAlreadyBookedException | PessimisticLockingFailureException e) {
                    // Expected concurrency failure when another thread wins the lock and completes booking
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // Check if inner cause is a concurrency/booking exception
                    if (e.getCause() instanceof SeatAlreadyBookedException || e.getMessage().contains("Seat already booked")) {
                        failureCount.incrementAndGet();
                    } else {
                        e.printStackTrace();
                        failureCount.incrementAndGet();
                    }
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // 6. Release all 10 threads simultaneously to maximize lock contention and race condition likelihood
        startLatch.countDown();

        // 7. Wait for all threads to finish execution
        boolean completedInTime = completionLatch.await(15, TimeUnit.SECONDS);
        assertTrue(completedInTime, "All concurrent booking threads should terminate within 15 seconds");
        executor.shutdown();

        // 8. Assertions: Exactly 1 booking succeeded and 9 failed due to locking/availability check
        System.out.println("Concurrent Booking Results -> Successful: " + successCount.get() + " | Failed: " + failureCount.get());
        assertEquals(1, successCount.get(), "Exactly ONE thread should succeed in booking the seat under high concurrency");
        assertEquals(threadCount - 1, failureCount.get(), "All other concurrent threads must fail with SeatAlreadyBookedException or lock failure");

        // 9. Verify database integrity: The seat is permanently marked as booked in the database
        boolean isBookedInDb = bookingSeatRepository.isSeatBooked(targetSeat.getId(), targetShow.getId());
        assertTrue(isBookedInDb, "Database must show the seat as BOOKED");
    }
}
