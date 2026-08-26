package com.bookmyshow.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ConnectionPoolMonitoringTest {

    @Autowired
    private ConnectionPoolMonitoringService poolMonitoringService;

    @Test
    @DisplayName("Verify connection pool monitoring service evaluates pool health and logs metrics without errors")
    void testConnectionPoolMonitoring() {
        assertNotNull(poolMonitoringService, "ConnectionPoolMonitoringService bean should be present");
        
        boolean healthy = poolMonitoringService.isPoolHealthy();
        assertTrue(healthy, "Connection pool should be evaluated as healthy under baseline test conditions");

        assertDoesNotThrow(() -> poolMonitoringService.logPoolMetrics(), 
                "Logging pool metrics should execute cleanly");

        int active = poolMonitoringService.getActiveConnections();
        int idle = poolMonitoringService.getIdleConnections();
        
        assertTrue(active >= 0, "Active connections count should be non-negative");
        assertTrue(idle >= 0, "Idle connections count should be non-negative");
    }
}
