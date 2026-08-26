package com.bookmyshow.service;

import com.bookmyshow.dto.TicketVerificationResponse;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.repository.BookingRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
public class TicketService {

    private final BookingRepository bookingRepository;

    public TicketService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * Step 2: Generate dynamic QR Code PNG byte array from ticketToken
     */
    public byte[] generateQrCodeImage(String ticketToken, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(ticketToken, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate QR code for token {}: {}", ticketToken, e.getMessage());
            throw new RuntimeException("Error generating QR Code: " + e.getMessage(), e);
        }
    }

    /**
     * Generate Base64 Data URI for QR code (useful for HTML emails & JSON responses)
     */
    public String generateQrCodeBase64(String ticketToken) {
        byte[] pngBytes = generateQrCodeImage(ticketToken, 250, 250);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
    }

    /**
     * Step 3: Ticket Verification API logic
     */
    @Transactional(readOnly = true)
    public TicketVerificationResponse verifyTicket(String ticketToken) {
        if (ticketToken == null || ticketToken.trim().isEmpty()) {
            return TicketVerificationResponse.invalid();
        }

        Optional<Booking> bookingOpt = bookingRepository.findByTicketToken(ticketToken);
        if (bookingOpt.isEmpty()) {
            log.warn("Verification failed: No booking found for token {}", ticketToken);
            return TicketVerificationResponse.invalid();
        }

        Booking booking = bookingOpt.get();
        if (!"BOOKED".equals(booking.getBookingStatus()) && !"CONFIRMED".equals(booking.getStatus())) {
            log.warn("Verification failed: Booking ID {} is not in BOOKED state (current: {})", booking.getId(), booking.getBookingStatus());
            return TicketVerificationResponse.invalid();
        }

        String customerName = "Clerk User (" + booking.getClerkUserId() + ")";
        if (booking.getUser() != null && booking.getUser().getName() != null) {
            customerName = booking.getUser().getName();
        } else if (booking.getUserEmail() != null) {
            customerName = booking.getUserEmail();
        }

        return new TicketVerificationResponse(
                "VALID",
                booking.getMovieTitle(),
                booking.getTheatreName(),
                booking.getShowDate(),
                booking.getShowTime(),
                booking.getSeatIds(),
                booking.getBookingStatus(),
                customerName,
                booking.getId(),
                booking.getTicketToken(),
                booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount(),
                booking.getCityName()
        );
    }

    /**
     * Step 5 & 7: Generate Professional PDF Ticket with security checks
     */
    @Transactional(readOnly = true)
    public byte[] generatePdfTicket(String ticketToken, String clerkUserId) {
        Booking booking = bookingRepository.findByTicketToken(ticketToken)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found for token: " + ticketToken));

        // Step 7: Security enforcement
        if (clerkUserId != null && !clerkUserId.trim().isEmpty() && booking.getClerkUserId() != null
                && !clerkUserId.equals(booking.getClerkUserId())) {
            throw new SecurityException("Unauthorized: You do not own this ticket.");
        }

        try {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            // Brand Colors
            Color primaryRed = new Color(229, 9, 20); // #E50914
            Color darkBg = new Color(21, 25, 34);     // #151922
            Color borderGray = new Color(44, 52, 68);

            // Fonts
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, primaryRed);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(34, 197, 94)); // Green

            // Header Paragraph
            Paragraph header = new Paragraph("CineX — E-Ticket", brandFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subHeader = new Paragraph("Official Cinema Entry Pass", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12, Color.GRAY));
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Create 2-column table: Left for info, Right for QR Code
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);
            mainTable.setWidths(new float[]{3.5f, 2f});

            // Left Box: Booking Info
            PdfPTable infoTable = new PdfPTable(1);
            infoTable.setWidthPercentage(100);

            addTableRow(infoTable, "MOVIE", booking.getMovieTitle(), labelFont, subHeaderFont);
            addTableRow(infoTable, "THEATRE & CITY", booking.getTheatreName() + " (" + (booking.getCityName() != null ? booking.getCityName() : "N/A") + ")", labelFont, valueFont);
            addTableRow(infoTable, "SHOW DATE & TIME", booking.getShowDate() + " | " + booking.getShowTime(), labelFont, valueFont);
            addTableRow(infoTable, "SEATS BOOKED", booking.getSeatIds(), labelFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryRed));
            addTableRow(infoTable, "AMOUNT PAID", "Rs. " + (booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount()), labelFont, valueFont);
            addTableRow(infoTable, "BOOKING ID", "CNX-" + booking.getId(), labelFont, valueFont);
            addTableRow(infoTable, "TICKET STATUS", booking.getBookingStatus(), labelFont, statusFont);

            PdfPCell leftCell = new PdfPCell(infoTable);
            leftCell.setBorderColor(borderGray);
            leftCell.setPadding(15);
            mainTable.addCell(leftCell);

            // Right Box: QR Code
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorderColor(borderGray);
            rightCell.setPadding(15);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            byte[] qrBytes = generateQrCodeImage(ticketToken, 180, 180);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(qrImage);

            Paragraph qrText = new Paragraph("Scan at Entrance", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK));
            qrText.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(qrText);

            Paragraph tokenText = new Paragraph(ticketToken.substring(0, Math.min(ticketToken.length(), 18)) + "...", FontFactory.getFont(FontFactory.COURIER, 8, Color.GRAY));
            tokenText.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(tokenText);

            mainTable.addCell(rightCell);
            document.add(mainTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Important: This E-Ticket is non-transferable. Please bring a valid ID along with this ticket. Arrive 15 minutes prior to showtime.", FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF ticket for token {}: {}", ticketToken, e.getMessage());
            throw new RuntimeException("Error generating PDF ticket: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generatePdfTicketByBookingId(Long bookingId, String clerkUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
        if (booking.getTicketToken() == null) {
            throw new ResourceNotFoundException("No ticket token generated yet for booking ID: " + bookingId);
        }
        return generatePdfTicket(booking.getTicketToken(), clerkUserId);
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(8);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value != null ? value : "N/A", valueFont));
        table.addCell(cell);
    }
}
