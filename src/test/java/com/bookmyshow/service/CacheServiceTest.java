package com.bookmyshow.service;

import com.bookmyshow.dto.MovieDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CacheServiceTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheService.evictAllCaches();
    }

    @Test
    @DisplayName("Verify Cache Statistics and Architectural Explanations")
    void testCacheStatisticsAndExplanation() {
        Map<String, Object> stats = cacheService.getCacheStatistics();

        assertNotNull(stats, "Cache stats should not be null");
        assertTrue(stats.containsKey("cacheType"), "Should contain cacheType");
        assertTrue(stats.containsKey("totalHits"), "Should contain totalHits");
        assertTrue(stats.containsKey("totalMisses"), "Should contain totalMisses");
        assertTrue(stats.containsKey("totalEvictions"), "Should contain totalEvictions");
        assertTrue(stats.containsKey("explanation"), "Should contain architectural explanation");

        Map<String, String> explanation = (Map<String, String>) stats.get("explanation");
        assertTrue(explanation.containsKey("Cache Hit"));
        assertTrue(explanation.containsKey("Cache Miss"));
        assertTrue(explanation.containsKey("Cache Eviction"));
        assertTrue(explanation.containsKey("TTL (Time-To-Live)"));
    }

    @Test
    @DisplayName("Verify @Cacheable Hit/Miss behavior on MovieService")
    void testMovieCachingBehavior() {
        // 1st call -> Cache Miss
        List<MovieDto> firstCall = movieService.getAllMovies();
        assertNotNull(firstCall);

        // 2nd call -> Cache Hit
        List<MovieDto> secondCall = movieService.getAllMovies();
        assertNotNull(secondCall);
        assertEquals(firstCall.size(), secondCall.size());

        Map<String, Object> stats = cacheService.getCacheStatistics();
        long totalHits = ((Number) stats.get("totalHits")).longValue();
        long totalMisses = ((Number) stats.get("totalMisses")).longValue();

        // At least 1 hit and 1 miss recorded
        assertTrue(totalHits >= 1, "Should record at least 1 cache hit");
        assertTrue(totalMisses >= 1, "Should record at least 1 cache miss");
    }

    @Test
    @DisplayName("Verify @CacheEvict when adding a movie")
    void testCacheEvictionOnAddMovie() {
        movieService.getAllMovies(); // Populate cache

        MovieDto newMovie = new MovieDto();
        newMovie.setTitle("Test Cached Movie");
        newMovie.setDescription("Testing cache eviction");
        newMovie.setDuration(130);
        newMovie.setLanguage("English");
        newMovie.setGenre("Sci-Fi");
        newMovie.setReleaseDate(LocalDate.now());

        movieService.addMovie(newMovie); // Triggers @CacheEvict

        Map<String, Object> stats = cacheService.getCacheStatistics();
        long totalEvictions = ((Number) stats.get("totalEvictions")).longValue();
        assertTrue(totalEvictions >= 1, "Should record at least 1 cache eviction upon modification");
    }
}
