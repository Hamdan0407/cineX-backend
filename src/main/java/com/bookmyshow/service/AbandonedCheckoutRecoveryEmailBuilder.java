package com.bookmyshow.service;

import com.bookmyshow.dto.AbandonedCheckoutRecoveryEmailContent;
import org.springframework.stereotype.Component;

@Component
public class AbandonedCheckoutRecoveryEmailBuilder {

    public static final String SUBJECT = "Your movie night is waiting \uD83C\uDFAC";

    public String buildHtml(AbandonedCheckoutRecoveryEmailContent content) {
        String name = content.getRecipientName() != null && !content.getRecipientName().isBlank()
                ? escape(content.getRecipientName())
                : "there";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Complete Your CineX Booking</title>
                </head>
                <body style="margin:0;padding:0;background-color:#0B0D12;font-family:'Inter',Arial,sans-serif;color:#FFFFFF;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#0B0D12;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background-color:#151922;border:1px solid #2C3444;border-radius:16px;overflow:hidden;">
                          <tr>
                            <td style="padding:28px 24px 12px;text-align:center;">
                              <div style="font-size:24px;font-weight:800;letter-spacing:0.14em;color:#E50914;">CINEX</div>
                              <p style="margin:8px 0 0;color:#B8C0CC;font-size:14px;">Your seats are still reserved for a little while</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:8px 24px 0;color:#FFFFFF;font-size:15px;line-height:1.6;">
                              <p style="margin:0 0 16px;">Hello %s,</p>
                              <p style="margin:0 0 16px;">You&rsquo;re just one step away from your movie plan.</p>
                              <p style="margin:0 0 12px;">Your selected seats for:</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 24px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#1C2230;border:1px solid #2C3444;border-radius:12px;">
                                <tr>
                                  <td style="padding:18px 20px;font-size:14px;line-height:1.7;">
                                    <p style="margin:0 0 8px;"><span style="color:#B8C0CC;">Movie</span><br><strong style="color:#FFFFFF;font-size:16px;">%s</strong></p>
                                    <p style="margin:0 0 8px;"><span style="color:#B8C0CC;">Theatre</span><br><strong style="color:#FFFFFF;">%s</strong></p>
                                    <p style="margin:0 0 8px;"><span style="color:#B8C0CC;">City</span><br><strong style="color:#FFFFFF;">%s</strong></p>
                                    <p style="margin:0 0 8px;"><span style="color:#B8C0CC;">Date</span><br><strong style="color:#FFFFFF;">%s</strong></p>
                                    <p style="margin:0 0 8px;"><span style="color:#B8C0CC;">Showtime</span><br><strong style="color:#FFFFFF;">%s</strong></p>
                                    <p style="margin:0;"><span style="color:#B8C0CC;">Seats</span><br><strong style="color:#22C55E;font-size:16px;">%s</strong></p>
                                    <p style="margin:8px 0 0;"><span style="color:#B8C0CC;">Total</span><br><strong style="color:#FFFFFF;">%s</strong></p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:24px;text-align:center;">
                              <p style="margin:0 0 18px;color:#B8C0CC;font-size:14px;line-height:1.6;">
                                Complete your booking before the seats become unavailable.
                              </p>
                              <a href="%s" style="display:inline-block;background-color:#E50914;color:#FFFFFF;text-decoration:none;padding:14px 28px;border-radius:8px;font-weight:700;font-size:15px;box-shadow:0 4px 15px rgba(229,9,20,0.35);">
                                Complete Your Booking
                              </a>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 24px 28px;color:#64748B;font-size:12px;line-height:1.6;text-align:center;border-top:1px solid #2C3444;">
                              <p style="margin:16px 0 0;">If you no longer want to book this show, you can safely ignore this email.</p>
                              <p style="margin:12px 0 0;">Regards,<br><strong style="color:#B8C0CC;">CineX Team</strong></p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                name,
                escape(content.getMovieTitle()),
                escape(content.getTheatreName()),
                escape(content.getCityName()),
                escape(content.getShowDate()),
                escape(content.getShowTime()),
                escape(content.getSeatNumbers()),
                escape(content.getAmount()),
                content.getRecoveryUrl()
        );
    }

    public String buildPlainText(AbandonedCheckoutRecoveryEmailContent content) {
        String name = content.getRecipientName() != null && !content.getRecipientName().isBlank()
                ? content.getRecipientName()
                : "there";

        return """
                Hello %s,

                You're just one step away from your movie plan.

                Your selected seats for:

                %s
                %s
                %s
                %s
                %s
                %s
                %s

                Complete your booking before the seats become unavailable.

                Complete your booking: %s

                If you no longer want to book this show, you can safely ignore this email.

                Regards,
                CineX Team
                """.formatted(
                name,
                content.getMovieTitle(),
                content.getTheatreName(),
                content.getCityName(),
                content.getShowDate(),
                content.getShowTime(),
                content.getSeatNumbers(),
                content.getAmount(),
                content.getRecoveryUrl()
        );
    }

    private static String escape(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
