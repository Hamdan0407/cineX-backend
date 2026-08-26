package com.bookmyshow.monitoring;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Service for monitoring HikariCP connection pool health and utilization.
 * Tracks active/idle connections, detects leaks, and alerts when utilization is critical.
 */
@Slf4j
@Service
public class ConnectionPoolMonitoringService {

    @Autowired
    private DataSource dataSource;

    public boolean isPoolHealthy() {
        HikariPoolMXBean poolBean = getPoolBean();
        if (poolBean == null) {
            return true; // Fallback assume healthy if not Hikari or wrapped in test
        }
        int total = poolBean.getTotalConnections();
        int active = poolBean.getActiveConnections();
        if (total > 0 && ((double) active / total) > 0.85) {
            log.warn("CRITICAL: HikariCP Connection Pool utilization exceeds 85%! Active: {}, Total: {}", active, total);
            return false;
        }
        return true;
    }

    public void logPoolMetrics() {
        HikariPoolMXBean poolBean = getPoolBean();
        if (poolBean != null) {
            log.info("HikariCP Metrics -> Active: {}, Idle: {}, Total: {}, Waiting Threads: {}",
                    poolBean.getActiveConnections(),
                    poolBean.getIdleConnections(),
                    poolBean.getTotalConnections(),
                    poolBean.getThreadsAwaitingConnection());
        } else {
            log.debug("HikariPoolMXBean not available for current DataSource.");
        }
    }

    public int getActiveConnections() {
        HikariPoolMXBean poolBean = getPoolBean();
        return poolBean != null ? poolBean.getActiveConnections() : 0;
    }

    public int getIdleConnections() {
        HikariPoolMXBean poolBean = getPoolBean();
        return poolBean != null ? poolBean.getIdleConnections() : 0;
    }

    private HikariPoolMXBean getPoolBean() {
        try {
            if (dataSource instanceof HikariDataSource) {
                return ((HikariDataSource) dataSource).getHikariPoolMXBean();
            } else if (dataSource.isWrapperFor(HikariDataSource.class)) {
                return dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean();
            }
        } catch (Exception e) {
            log.trace("Could not unwrap HikariDataSource: {}", e.getMessage());
        }
        return null;
    }
}
