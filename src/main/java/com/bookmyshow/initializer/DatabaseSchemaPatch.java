package com.bookmyshow.initializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures newer columns exist on databases created before catalog-active filtering.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaPatch {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void applyPatches() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE movies ADD COLUMN IF NOT EXISTS catalog_active BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute(
                    "UPDATE movies SET catalog_active = FALSE WHERE catalog_active IS NULL");
            log.info("Verified movies.catalog_active column");
        } catch (Exception ex) {
            log.warn("Could not patch movies.catalog_active column: {}", ex.getMessage());
        }
        patchSeatLayoutColumns();
        patchScreenLayoutColumns();
    }

    private void patchSeatLayoutColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE seats ADD COLUMN IF NOT EXISTS row_label VARCHAR(8)");
            jdbcTemplate.execute("ALTER TABLE seats ADD COLUMN IF NOT EXISTS row_index INTEGER");
            jdbcTemplate.execute("ALTER TABLE seats ADD COLUMN IF NOT EXISTS column_index INTEGER");
            jdbcTemplate.execute("ALTER TABLE seats ADD COLUMN IF NOT EXISTS wheelchair_accessible BOOLEAN DEFAULT FALSE");
            log.info("Verified seats layout columns");
        } catch (Exception ex) {
            log.warn("Could not patch seats layout columns: {}", ex.getMessage());
        }
    }

    private void patchScreenLayoutColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE screens ADD COLUMN IF NOT EXISTS layout_profile VARCHAR(64)");
            log.info("Verified screens.layout_profile column");
        } catch (Exception ex) {
            log.warn("Could not patch screens.layout_profile column: {}", ex.getMessage());
        }
    }
}
