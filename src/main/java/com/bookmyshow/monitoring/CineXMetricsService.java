package com.bookmyshow.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class CineXMetricsService {

    private final Counter bookingAttemptsCounter;
    private final Counter successfulBookingsCounter;
    private final Counter failedBookingsCounter;
    private final Counter ticketGeneratedCounter;
    private final DistributionSummary bookingAmountSummary;
    private final Timer bookingProcessTimer;

    public CineXMetricsService(MeterRegistry meterRegistry) {
        this.bookingAttemptsCounter = Counter.builder("cinex.bookings.attempts.total")
                .description("Total number of ticket booking attempts initiated")
                .tag("platform", "web")
                .register(meterRegistry);

        this.successfulBookingsCounter = Counter.builder("cinex.bookings.successful.total")
                .description("Total number of successfully confirmed bookings")
                .tag("status", "SUCCESS")
                .register(meterRegistry);

        this.failedBookingsCounter = Counter.builder("cinex.bookings.failed.total")
                .description("Total number of failed or cancelled bookings")
                .tag("status", "FAILED")
                .register(meterRegistry);

        this.ticketGeneratedCounter = Counter.builder("cinex.tickets.generated.total")
                .description("Total secure digital QR tickets generated")
                .register(meterRegistry);

        this.bookingAmountSummary = DistributionSummary.builder("cinex.booking.amount")
                .description("Distribution of transaction amounts in checkout")
                .baseUnit("INR")
                .register(meterRegistry);

        this.bookingProcessTimer = Timer.builder("cinex.booking.process.time")
                .description("Time taken to process and verify payment and generate ticket")
                .register(meterRegistry);
    }

    public void recordBookingAttempt() {
        bookingAttemptsCounter.increment();
    }

    public void recordSuccessfulBooking(BigDecimal amount) {
        successfulBookingsCounter.increment();
        if (amount != null) {
            bookingAmountSummary.record(amount.doubleValue());
        }
    }

    public void recordFailedBooking() {
        failedBookingsCounter.increment();
    }

    public void recordTicketGenerated() {
        ticketGeneratedCounter.increment();
    }

    public void recordBookingDuration(long durationMillis) {
        bookingProcessTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }
}
