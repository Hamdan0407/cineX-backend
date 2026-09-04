package com.bookmyshow.scheduler;

import com.bookmyshow.config.AbandonedCheckoutProperties;
import com.bookmyshow.service.AbandonedCheckoutRecoveryProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedCheckoutRecoveryScheduler implements SchedulingConfigurer {

    private final AbandonedCheckoutProperties properties;
    private final AbandonedCheckoutRecoveryProcessor recoveryProcessor;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        long intervalMillis = properties.getProcessorIntervalMillis();
        taskRegistrar.addFixedDelayTask(this::runRecoveryProcessor, intervalMillis);
    }

    void runRecoveryProcessor() {
        if (!properties.isProcessorEnabled()) {
            return;
        }
        try {
            recoveryProcessor.processEligibleCheckouts();
        } catch (Exception ex) {
            log.error("Abandoned checkout recovery scheduler failed: {}", ex.getMessage());
        }
    }
}
