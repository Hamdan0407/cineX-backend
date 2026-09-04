package com.bookmyshow.service;

import com.bookmyshow.config.CinexMailProperties;
import com.bookmyshow.dto.AbandonedCheckoutRecoveryEmailContent;
import com.bookmyshow.dto.BookingResponse;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.entity.Show;
import com.bookmyshow.exception.EmailDeliveryException;
import com.bookmyshow.repository.ShowRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender emailSender;
    private final ShowRepository showRepository;
    private final CinexMailProperties mailProperties;

    @org.springframework.beans.factory.annotation.Value("${cinex.api-url:http://localhost:8081}")
    private String apiUrl;

    public EmailService(ObjectProvider<JavaMailSender> emailSenderProvider,
                        ShowRepository showRepository,
                        CinexMailProperties mailProperties) {
        this.emailSender = emailSenderProvider.getIfAvailable();
        this.showRepository = showRepository;
        this.mailProperties = mailProperties;
    }

    @Async
    public void sendBookingConfirmation(String toEmail, BookingResponse booking) {
        Show show = showRepository.findById(booking.getShowId()).orElse(null);
        String movieName = (show != null && show.getMovie() != null) ? show.getMovie().getTitle() : "N/A";
        String showTime = (show != null) ? show.getShowDate() + " " + show.getShowTime() : "N/A";
        String seatNumbers = String.join(", ", booking.getSeatNumbers());

        String emailBody = String.format(
            "Booking Confirmed!\n\n" +
            "Booking ID: %d\n" +
            "Movie: %s\n" +
            "Show Time: %s\n" +
            "Seats: %s\n" +
            "Total Amount: $%.2f\n\n" +
            "Enjoy the movie!",
            booking.getBookingId(),
            movieName,
            showTime,
            seatNumbers,
            booking.getTotalAmount()
        );

        if (emailSender == null) {
            logger.warn("JavaMailSender is not configured. Falling back to logging booking confirmation.");
            logger.info("Email Content:\nTo: {}\nSubject: BookMyShow Booking Confirmation - {}\n\n{}",
                toEmail, booking.getBookingId(), emailBody);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            applyFrom(message);
            message.setTo(toEmail);
            message.setSubject("BookMyShow Booking Confirmation - " + booking.getBookingId());
            message.setText(emailBody);
            
            emailSender.send(message);
            logger.info("Sent booking confirmation email to {}", toEmail);
        } catch (MailException e) {
            logger.warn("Mail credentials not set or email sending failed. Falling back to logging.");
            logger.info("Email Content:\nTo: {}\nSubject: BookMyShow Booking Confirmation - {}\n\n{}", 
                toEmail, booking.getBookingId(), emailBody);
        } catch (Exception e) {
            logger.error("Error occurred while sending email: ", e);
        }
    }

    @Async
    public void sendHtmlTicketConfirmation(String toEmail, Booking booking, String qrCodeBase64, String ticketToken) {
        if (emailSender == null) {
            logger.warn("JavaMailSender is not configured. Falling back to logging HTML ticket.");
            logger.info("HTML Ticket Content generated for Booking ID {}: Movie: {}, Seats: {}, Amount: Rs. {}",
                booking.getId(), booking.getMovieTitle(), booking.getSeatIds(), booking.getAmount());
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            applyFrom(helper);
            helper.setTo(toEmail);
            helper.setSubject("CineX E-Ticket Confirmation — " + booking.getMovieTitle());

            String posterImg = "";
            if (booking.getPosterPath() != null && !booking.getPosterPath().isEmpty()) {
                String posterUrl = booking.getPosterPath().startsWith("http") ? booking.getPosterPath() : "https://image.tmdb.org/t/p/w300" + booking.getPosterPath();
                posterImg = String.format("<img src=\"%s\" alt=\"%s Poster\" style=\"max-width: 140px; border-radius: 8px; margin-bottom: 15px; box-shadow: 0 4px 12px rgba(0,0,0,0.5);\" />", posterUrl, booking.getMovieTitle());
            }

            String htmlContent = String.format(
                "<!DOCTYPE html>" +
                "<html><head><meta charset=\"utf-8\"><title>CineX E-Ticket</title></head>" +
                "<body style=\"background-color: #0B0D12; color: #FFFFFF; font-family: 'Inter', Arial, sans-serif; margin: 0; padding: 30px;\">" +
                "  <div style=\"max-width: 580px; margin: 0 auto; background-color: #151922; border: 1px solid #2C3444; border-radius: 16px; padding: 30px; text-align: center; box-shadow: 0 10px 30px rgba(0,0,0,0.7);\">" +
                "    <h1 style=\"color: #E50914; margin-top: 0; font-size: 28px; font-weight: 800; letter-spacing: 1px;\">CINEX</h1>" +
                "    <p style=\"color: #B8C0CC; font-size: 14px; margin-bottom: 25px;\">Your Official Cinema Ticket Pass</p>" +
                "    %s" +
                "    <h2 style=\"color: #FFFFFF; margin: 10px 0 5px 0; font-size: 22px;\">%s</h2>" +
                "    <p style=\"color: #E50914; font-weight: bold; font-size: 16px; margin: 0 0 20px 0;\">%s</p>" +
                "    <div style=\"background-color: #1C2230; border-radius: 12px; padding: 20px; text-align: left; margin-bottom: 25px; border: 1px solid #2C3444;\">" +
                "      <p style=\"margin: 8px 0; font-size: 14px;\"><span style=\"color: #B8C0CC;\">City & Date:</span> <strong style=\"color: #FFF;\">%s | %s</strong></p>" +
                "      <p style=\"margin: 8px 0; font-size: 14px;\"><span style=\"color: #B8C0CC;\">Show Time:</span> <strong style=\"color: #FFF;\">%s</strong></p>" +
                "      <p style=\"margin: 8px 0; font-size: 14px;\"><span style=\"color: #B8C0CC;\">Seats Booked:</span> <strong style=\"color: #22C55E; font-size: 16px;\">%s</strong></p>" +
                "      <p style=\"margin: 8px 0; font-size: 14px;\"><span style=\"color: #B8C0CC;\">Amount Paid:</span> <strong style=\"color: #FFF;\">Rs. %.2f</strong></p>" +
                "      <p style=\"margin: 8px 0; font-size: 14px;\"><span style=\"color: #B8C0CC;\">Booking ID:</span> <strong style=\"color: #B8C0CC;\">CNX-%d</strong></p>" +
                "    </div>" +
                "    <div style=\"background-color: #FFFFFF; padding: 20px; border-radius: 12px; display: inline-block; margin-bottom: 20px;\">" +
                "      <img src=\"%s\" alt=\"QR Code\" style=\"width: 180px; height: 180px; display: block; margin: 0 auto;\" />" +
                "      <p style=\"color: #000000; font-size: 11px; font-weight: bold; margin: 10px 0 0 0;\">SCAN AT CINEMA ENTRANCE</p>" +
                "    </div>" +
                "    <div style=\"margin-top: 15px;\">" +
                "      <a href=\"%s/api/tickets/download/%s\" style=\"background-color: #E50914; color: #FFFFFF; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; font-size: 15px; display: inline-block; box-shadow: 0 4px 15px rgba(229, 9, 20, 0.4);\">Download PDF Ticket</a>" +
                "    </div>" +
                "    <p style=\"color: #64748B; font-size: 12px; margin-top: 30px; border-top: 1px solid #2C3444; padding-top: 20px;\">Thank you for choosing CineX. Please present this QR code at the entrance gate. Enjoy your movie!</p>" +
                "  </div>" +
                "</body></html>",
                posterImg,
                booking.getMovieTitle(),
                booking.getTheatreName(),
                booking.getCityName() != null ? booking.getCityName() : "Mumbai",
                booking.getShowDate(),
                booking.getShowTime(),
                booking.getSeatIds(),
                booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount(),
                booking.getId(),
                qrCodeBase64,
                apiUrl,
                ticketToken
            );

            helper.setText(htmlContent, true);
            emailSender.send(message);
            logger.info("Sent HTML ticket confirmation email to {}", toEmail);
        } catch (MailException e) {
            logger.warn("Mail credentials not set or email sending failed. Falling back to logging HTML ticket.");
            logger.info("HTML Ticket Content generated for Booking ID {}: Movie: {}, Seats: {}, Amount: Rs. {}", 
                booking.getId(), booking.getMovieTitle(), booking.getSeatIds(), booking.getAmount());
        } catch (Exception e) {
            logger.error("Error occurred while sending HTML ticket email: ", e);
        }
    }

    /**
     * Sends abandoned-checkout recovery email. Synchronous so the processor can retry on failure.
     */
    public void sendAbandonedCheckoutRecoveryEmail(String toEmail,
                                                   AbandonedCheckoutRecoveryEmailContent content,
                                                   String htmlBody,
                                                   String plainTextBody) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new EmailDeliveryException("Recipient email is required");
        }

        if (emailSender == null) {
            logger.warn("JavaMailSender is not configured. Logging abandoned-checkout recovery email instead.");
            logger.info("Recovery email to {} | subject: {} | url: {}",
                    AbandonedCheckoutRecoveryProcessor.maskEmail(toEmail),
                    AbandonedCheckoutRecoveryEmailBuilder.SUBJECT,
                    content.getRecoveryUrl());
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            applyFrom(helper);
            helper.setTo(toEmail);
            helper.setSubject(AbandonedCheckoutRecoveryEmailBuilder.SUBJECT);
            helper.setText(plainTextBody, htmlBody);
            emailSender.send(message);
            logger.info("Sent abandoned-checkout recovery email to {}", AbandonedCheckoutRecoveryProcessor.maskEmail(toEmail));
        } catch (MailException | jakarta.mail.MessagingException e) {
            throw new EmailDeliveryException("Failed to send abandoned-checkout recovery email", e);
        }
    }

    private void applyFrom(SimpleMailMessage message) {
        if (mailProperties.getFrom() != null && !mailProperties.getFrom().isBlank()) {
            message.setFrom(mailProperties.getFrom());
        }
    }

    private void applyFrom(MimeMessageHelper helper) throws jakarta.mail.MessagingException {
        if (mailProperties.getFrom() != null && !mailProperties.getFrom().isBlank()) {
            String fromName = mailProperties.getFromName() != null ? mailProperties.getFromName() : "CineX";
            try {
                helper.setFrom(new InternetAddress(mailProperties.getFrom(), fromName, "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                helper.setFrom(mailProperties.getFrom());
            }
        }
    }
}
