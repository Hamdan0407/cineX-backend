package com.bookmyshow.support;

import com.bookmyshow.entity.Show;
import com.bookmyshow.repository.ShowRepository;
import com.bookmyshow.service.ShowService;
import com.bookmyshow.service.TmdbCatalogSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Helpers for integration tests that need CineX show inventory without bulk startup seeding.
 */
public abstract class ShowInventoryTestSupport {

    protected static final long SAMPLE_TMDB_ID = 653346L;
    protected static final String SAMPLE_CITY = "Chennai";

    @DynamicPropertySource
    static void forceTestTmdbKey(DynamicPropertyRegistry registry) {
        registry.add("tmdb.api.key", () -> "test-key");
    }

    @Autowired
    protected ShowService showService;

    @Autowired
    protected ShowRepository showRepository;

    @Autowired
    protected TmdbCatalogSyncService tmdbCatalogSyncService;

    protected void ensureCatalogMovies() {
        tmdbCatalogSyncService.syncAndGetActiveMovies(6);
    }

    protected void ensureSampleShowInventory() {
        ensureCatalogMovies();
        showService.getShowsByTmdbId(SAMPLE_TMDB_ID, SAMPLE_CITY, null);
    }

    protected Show requireSampleShow() {
        ensureSampleShowInventory();
        List<Show> shows = showRepository.findAll();
        assertFalse(shows.isEmpty(), "Expected on-demand show inventory for tests");
        return shows.get(0);
    }
}
