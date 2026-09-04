package com.bookmyshow.scheduler;

import com.bookmyshow.config.AbandonedCheckoutProperties;
import com.bookmyshow.service.AbandonedCheckoutRecoveryProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbandonedCheckoutRecoverySchedulerTest {

    @Mock
    private AbandonedCheckoutProperties properties;

    @Mock
    private AbandonedCheckoutRecoveryProcessor recoveryProcessor;

    @InjectMocks
    private AbandonedCheckoutRecoveryScheduler scheduler;

    @Test
    void runRecoveryProcessor_skipsWhenDisabled() {
        when(properties.isProcessorEnabled()).thenReturn(false);

        scheduler.runRecoveryProcessor();

        verifyNoInteractions(recoveryProcessor);
    }

    @Test
    void runRecoveryProcessor_invokesProcessorWhenEnabled() {
        when(properties.isProcessorEnabled()).thenReturn(true);

        scheduler.runRecoveryProcessor();

        verify(recoveryProcessor).processEligibleCheckouts();
    }

    @Test
    void configureTasks_registersFixedDelayFromProperties() {
        when(properties.getProcessorIntervalMillis()).thenReturn(120_000L);

        org.springframework.scheduling.config.ScheduledTaskRegistrar registrar =
                new org.springframework.scheduling.config.ScheduledTaskRegistrar();
        scheduler.configureTasks(registrar);
        registrar.afterPropertiesSet();

        assertEquals(1, registrar.getFixedDelayTaskList().size());
    }
}
