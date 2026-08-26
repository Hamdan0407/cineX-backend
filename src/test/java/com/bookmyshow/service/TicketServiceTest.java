package com.bookmyshow.service;

import com.bookmyshow.dto.TicketVerificationResponse;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private BookingRepository bookingRepository;

    private Booking bookedBooking;
    private Booking failedBooking;

    @BeforeEach
    void setUp() {
        bookedBooking = new Booking();
        bookedBooking.setClerkUserId("clerk_user_ticket_1");
        bookedBooking.setMovieTitle("Interstellar");
        bookedBooking.setTheatreName("IMAX Theatre");
        bookedBooking.setShowDate("2026-07-10");
        bookedBooking.setShowTime("18:00");
        bookedBooking.setSeatIds("B1,B2");
        bookedBooking.setAmount(800.00);
        bookedBooking.setTotalAmount(800.00);
        bookedBooking.setBookingStatus("BOOKED");
        bookedBooking.setPaymentStatus("SUCCESS");
        bookedBooking.setTicketToken("TOKEN-VALID-" + UUID.randomUUID().toString());
        bookedBooking = bookingRepository.save(bookedBooking);

        failedBooking = new Booking();
        failedBooking.setClerkUserId("clerk_user_ticket_2");
        failedBooking.setMovieTitle("Avatar 3");
        failedBooking.setBookingStatus("FAILED");
        failedBooking.setPaymentStatus("FAILED");
        failedBooking.setTicketToken("TOKEN-FAIL-" + UUID.randomUUID().toString());
        failedBooking = bookingRepository.save(failedBooking);
    }

    @Test
    void testQrGeneration() {
        byte[] qrBytes = ticketService.generateQrCodeImage(bookedBooking.getTicketToken(), 250, 250);
        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 0, "QR code byte array should not be empty");

        String qrBase64 = ticketService.generateQrCodeBase64(bookedBooking.getTicketToken());
        assertNotNull(qrBase64);
        assertTrue(qrBase64.startsWith("data:image/png;base64,"), "Should be formatted as Base64 Data URI");
    }

    @Test
    void testTokenGeneration_UniqueAndSecure() {
        assertNotNull(bookedBooking.getTicketToken());
        assertNotEquals(String.valueOf(bookedBooking.getId()), bookedBooking.getTicketToken(), "Token should NOT simply be booking ID");
    }

    @Test
    void testTicketVerification_ValidToken() {
        TicketVerificationResponse response = ticketService.verifyTicket(bookedBooking.getTicketToken());
        assertNotNull(response);
        assertEquals("VALID", response.getVerificationStatus());
        assertEquals("Interstellar", response.getMovie());
        assertEquals("IMAX Theatre", response.getTheatre());
        assertEquals("B1,B2", response.getSeats());
        assertEquals(bookedBooking.getId(), response.getBookingId());
    }

    @Test
    void testTicketVerification_InvalidToken() {
        TicketVerificationResponse response = ticketService.verifyTicket("NON-EXISTENT-TOKEN-999");
        assertNotNull(response);
        assertEquals("INVALID TICKET", response.getVerificationStatus());
    }

    @Test
    void testTicketVerification_PendingOrFailedBooking() {
        TicketVerificationResponse response = ticketService.verifyTicket(failedBooking.getTicketToken());
        assertNotNull(response);
        assertEquals("INVALID TICKET", response.getVerificationStatus(), "Should return INVALID TICKET if booking status is not BOOKED");
    }

    @Test
    void testPdfGeneration_Success() {
        byte[] pdfBytes = ticketService.generatePdfTicket(bookedBooking.getTicketToken(), "clerk_user_ticket_1");
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF byte array should not be empty");
        // Check standard PDF header magic bytes '%PDF'
        String pdfHeader = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", pdfHeader, "Generated file should be a valid PDF document");
    }

    @Test
    void testPdfGeneration_UnauthorizedAccess() {
        assertThrows(SecurityException.class, () ->
                ticketService.generatePdfTicket(bookedBooking.getTicketToken(), "hacker_user_id")
        );
    }
}
