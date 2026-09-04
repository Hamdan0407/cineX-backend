package com.bookmyshow.service;

import com.bookmyshow.entity.Movie;
import com.bookmyshow.repository.MovieRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TmdbCatalogSyncServiceTest {

    @DynamicPropertySource
    static void forceTestTmdbKey(DynamicPropertyRegistry registry) {
        registry.add("tmdb.api.key", () -> "test-key");
    }

    @Autowired
    private TmdbCatalogSyncService tmdbCatalogSyncService;

    @Autowired
    private MovieRepository movieRepository;

    @Test
    @DisplayName("Sync upserts movies by unique tmdbId without duplicates")
    void syncDoesNotCreateDuplicateTmdbMovies() {
        Map<Long, Movie> first = tmdbCatalogSyncService.syncAndGetActiveMovies(6);
        assertFalse(first.isEmpty());

        long countWithTmdb = movieRepository.findAll().stream()
                .filter(movie -> movie.getTmdbId() != null)
                .count();

        Map<Long, Movie> second = tmdbCatalogSyncService.syncAndGetActiveMovies(6);
        assertEquals(first.size(), second.size());

        long countAfter = movieRepository.findAll().stream()
                .filter(movie -> movie.getTmdbId() != null)
                .count();
        assertEquals(countWithTmdb, countAfter);
    }

    @Test
    @DisplayName("Synced movies are marked catalog active")
    void syncedMoviesAreCatalogActive() {
        Map<Long, Movie> active = tmdbCatalogSyncService.syncAndGetActiveMovies(3);
        active.values().forEach(movie ->
                assertTrue(Boolean.TRUE.equals(movie.getCatalogActive())));
    }
}
