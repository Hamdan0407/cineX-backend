package com.bookmyshow.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LoggingAndMdcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Verify MdcLoggingFilter Generates Unique X-Trace-Id When Not Provided")
    void testTraceIdGeneration() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn();

        String traceId = result.getResponse().getHeader("X-Trace-Id");
        assertNotNull(traceId);
        assertFalse(traceId.trim().isEmpty());
    }

    @Test
    @DisplayName("Verify MdcLoggingFilter Preserves Custom X-Trace-Id Header")
    void testCustomTraceIdPreservation() throws Exception {
        String customTraceId = "TEST-TRACE-12345";
        mockMvc.perform(get("/api/movies")
                        .header("X-Trace-Id", customTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", customTraceId));
    }
}
