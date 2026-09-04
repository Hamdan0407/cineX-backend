package com.bookmyshow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "abandoned-checkout")
public class AbandonedCheckoutProperties {

    /**
     * Minutes to wait after a checkout becomes incomplete before sending recovery email.
     */
    private int recoveryDelayMinutes = 30;

    /**
     * How often the scheduled processor runs.
     */
    private int processorIntervalMinutes = 10;

    /**
     * CineX frontend base URL for recovery links (no trailing slash).
     */
    private String frontendUrl = "http://localhost:5173";

    /**
     * Disable scheduled processing (useful in tests).
     */
    private boolean processorEnabled = true;

    public Duration getRecoveryDelay() {
        return Duration.ofMinutes(recoveryDelayMinutes);
    }

    public Duration getProcessorInterval() {
        return Duration.ofMinutes(processorIntervalMinutes);
    }

    public long getProcessorIntervalMillis() {
        return getProcessorInterval().toMillis();
    }
}
