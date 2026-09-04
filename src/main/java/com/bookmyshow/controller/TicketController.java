package com.bookmyshow.controller;

import com.bookmyshow.dto.TicketVerificationResponse;
import com.bookmyshow.service.AdminAuthService;
import com.bookmyshow.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Digital Ticketing & QR Verification", description = "Endpoints for QR code generation, ticket verification, and PDF download")
public class TicketController {

    private final TicketService ticketService;
    private final AdminAuthService adminAuthService;

    public TicketController(TicketService ticketService, AdminAuthService adminAuthService) {
        this.ticketService = ticketService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/verify/{ticketToken}")
    @Operation(summary = "Verify a ticket token and return booking details if valid")
    public ResponseEntity<TicketVerificationResponse> verifyTicket(@PathVariable String ticketToken) {
        adminAuthService.validateTicketAccess(ticketToken);
        TicketVerificationResponse response = ticketService.verifyTicket(ticketToken);
        if ("INVALID TICKET".equals(response.getVerificationStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/qr/{ticketToken}", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate dynamic QR code PNG image for a ticket token")
    public ResponseEntity<byte[]> getQrCodeImage(@PathVariable String ticketToken) {
        adminAuthService.validateTicketAccess(ticketToken);
        byte[] imageBytes = ticketService.generateQrCodeImage(ticketToken, 250, 250);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("max-age=3600");
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/download/{ticketToken}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download professional PDF ticket by ticket token")
    public ResponseEntity<byte[]> downloadPdfTicket(@PathVariable String ticketToken) {
        adminAuthService.validateTicketAccess(ticketToken);
        String clerkUserId = adminAuthService.getAuthenticatedClerkUserId();
        byte[] pdfBytes = ticketService.generatePdfTicket(ticketToken, clerkUserId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "CineX-Ticket-" + ticketToken.substring(0, Math.min(ticketToken.length(), 8)) + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/download-by-booking/{bookingId}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download professional PDF ticket by booking ID")
    public ResponseEntity<byte[]> downloadPdfTicketByBookingId(@PathVariable Long bookingId) {
        adminAuthService.validateBookingAccess(bookingId);
        String clerkUserId = adminAuthService.getAuthenticatedClerkUserId();
        byte[] pdfBytes = ticketService.generatePdfTicketByBookingId(bookingId, clerkUserId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "CineX-Ticket-CNX-" + bookingId + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
