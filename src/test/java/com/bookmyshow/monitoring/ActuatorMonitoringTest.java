package com.bookmyshow.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ActuatorMonitoringTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CineXMetricsService metricsService;

    @Test
    @DisplayName("Verify /actuator/health returns UP with Custom Health Details")
    void testActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.cineX.details.database").value("UP"))
                .andExpect(jsonPath("$.components.cineX.details.razorpayGateway").exists());
    }

    @Test
    @DisplayName("Verify /actuator/metrics contains custom business metrics")
    void testActuatorMetricsEndpoint() throws Exception {
        // Trigger a metric event to ensure registration
        metricsService.recordBookingAttempt();

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cinex.bookings.attempts.total")))
                .andExpect(content().string(containsString("cinex.tickets.generated.total")));
    }

    @Test
    @DisplayName("Verify /actuator/prometheus exports valid Prometheus scrape format")
    void testActuatorPrometheusEndpoint() throws Exception {
        metricsService.recordBookingAttempt();

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cinex_bookings_attempts_total")));
    }
}
