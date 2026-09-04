package com.bookmyshow.controller;

import com.bookmyshow.dto.BookingRequest;
import com.bookmyshow.dto.BookingResponse;
import com.bookmyshow.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import org.springframework.data.domain.Page;

@Tag(name = "Booking API", description = "Operations related to booking api")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final com.bookmyshow.service.AdminAuthService adminAuthService;

    public BookingController(BookingService bookingService, com.bookmyshow.service.AdminAuthService adminAuthService) {
        this.bookingService = bookingService;
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "Create a new booking", description = "Initiates ticket reservation, locks selected seats, and creates a pending Razorpay order.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking created successfully"),
        @ApiResponse(responseCode = "400", description = "Seats already booked or invalid request")
    })
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        String authenticatedClerkUserId = adminAuthService.getAuthenticatedClerkUserId();
        BookingResponse response = bookingService.createBooking(request, authenticatedClerkUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all bookings (Admin only)")
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @Operation(summary = "Search bookings (Admin only)")
    @GetMapping("/search")
    public ResponseEntity<List<BookingResponse>> searchBookings(@RequestParam(required = false) String query) {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(bookingService.searchBookings(query));
    }

    @Operation(summary = "Get paginated bookings with size and sorting (Admin only)")
    @GetMapping("/paginated")
    public ResponseEntity<Page<BookingResponse>> getBookingsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(bookingService.getBookingsPaginated(page, size, sortBy, sortDir));
    }

    @Operation(summary = "Search paginated bookings with size and sorting (Admin only)")
    @GetMapping("/search/paginated")
    public ResponseEntity<Page<BookingResponse>> searchBookingsPaginated(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(bookingService.searchBookingsPaginated(query, page, size, sortBy, sortDir));
    }

    @Operation(summary = "Update booking status (Admin only)")
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(@PathVariable Long bookingId,
                                                 @RequestParam(required = false) String bookingStatus,
                                                 @RequestParam(required = false) String paymentStatus) {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(bookingService.updateBookingStatus(bookingId, bookingStatus, paymentStatus));
    }

    @Operation(summary = "Get booking by ID", description = "Fetches complete booking details including seat numbers, payment status, and QR ticket token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully found booking"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long bookingId) {
        BookingResponse booking = bookingService.getBooking(bookingId);
        if (booking.getClerkUserId() != null) {
            adminAuthService.validateOwnershipOrAdmin(booking.getClerkUserId());
        } else {
            adminAuthService.validateAdmin();
        }
        return ResponseEntity.ok(booking);
    }

    @Operation(summary = "Get bookings by User ID", description = "Retrieves booking history for a database User ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user bookings")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@PathVariable Long userId) {
        adminAuthService.validateUserIdOwnership(userId);
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    @Operation(summary = "Get bookings by Clerk User ID", description = "Retrieves booking history for an authenticated Clerk account ordered by booking date.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved Clerk user bookings")
    })
    @GetMapping("/clerk/{clerkUserId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByClerkUserId(@PathVariable String clerkUserId) {
        adminAuthService.validateOwnershipOrAdmin(clerkUserId);
        return ResponseEntity.ok(bookingService.getBookingsByClerkUserId(clerkUserId));
    }

    @Operation(summary = "Cancel booking by ID", description = "Cancels an existing booking and releases reserved seats if applicable.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot cancel booking in current state")
    })
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {
        BookingResponse booking = bookingService.getBooking(bookingId);
        if (booking.getClerkUserId() != null) {
            adminAuthService.validateOwnershipOrAdmin(booking.getClerkUserId());
        } else {
            adminAuthService.validateAdmin();
        }
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok("Booking cancelled successfully");
    }
}
