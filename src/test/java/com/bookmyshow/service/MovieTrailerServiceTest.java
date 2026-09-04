package com.bookmyshow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bookmyshow.config.CinexMediaProperties;
import com.bookmyshow.dto.TrailerMediaResponse;
import com.bookmyshow.entity.Movie;
import com.bookmyshow.repository.MovieRepository;

@ExtendWith(MockitoExtension.class)
class MovieTrailerServiceTest {

    static final String SPIDER_MAN_TRAILER_OBJECT_KEY =
            "trailers/SPIDER-MAN_ BRAND NEW DAY \u2013 New Trailer (4K).mp4";

    @Mock
    private MovieRepository movieRepository;

    private CinexMediaProperties mediaProperties;
    private MediaDeliveryService mediaDeliveryService;
    private MovieTrailerService movieTrailerService;

    @BeforeEach
    void setUp() {
        mediaProperties = new CinexMediaProperties();
        mediaProperties.setCloudfrontBaseUrl("https://d1al8zqo1izqiu.cloudfront.net");
        mediaProperties.setTrailerMappings(Map.of(969681L, SPIDER_MAN_TRAILER_OBJECT_KEY));
        mediaDeliveryService = new MediaDeliveryService(mediaProperties);
        movieTrailerService = new MovieTrailerService(movieRepository, mediaProperties, mediaDeliveryService);
    }

    @Test
    void resolvesConfiguredTmdbTrailerMapping() {
        when(movieRepository.findByTmdbId(969681L)).thenReturn(Optional.empty());

        TrailerMediaResponse response = movieTrailerService.resolveTrailerForTmdbId(969681L);

        assertTrue(response.isAvailable());
        assertEquals(969681L, response.getTmdbId());
        assertEquals(SPIDER_MAN_TRAILER_OBJECT_KEY, response.getTrailerObjectKey());
        assertEquals(
                "https://d1al8zqo1izqiu.cloudfront.net/"
                        + MediaDeliveryService.encodeObjectKeyForUrl(SPIDER_MAN_TRAILER_OBJECT_KEY),
                response.getTrailerPlaybackUrl());
    }

    @Test
    void prefersMovieStoredTrailerObjectKeyOverConfigMapping() {
        Movie movie = new Movie();
        movie.setId(12L);
        movie.setTmdbId(969681L);
        movie.setTrailerUrl("trailers/custom-trailer.mp4");
        when(movieRepository.findByTmdbId(969681L)).thenReturn(Optional.of(movie));

        TrailerMediaResponse response = movieTrailerService.resolveTrailerForTmdbId(969681L);

        assertEquals("trailers/custom-trailer.mp4", response.getTrailerObjectKey());
        assertEquals(
                "https://d1al8zqo1izqiu.cloudfront.net/trailers/custom-trailer.mp4",
                response.getTrailerPlaybackUrl());
    }

    @Test
    void returnsUnavailableWhenNoMappingExists() {
        when(movieRepository.findByTmdbId(42L)).thenReturn(Optional.empty());

        TrailerMediaResponse response = movieTrailerService.resolveTrailerForTmdbId(42L);

        assertFalse(response.isAvailable());
        assertEquals(42L, response.getTmdbId());
    }

    @Test
    void preservesExternalTrailerUrlsStoredOnMovie() {
        Movie movie = new Movie();
        movie.setId(7L);
        movie.setTmdbId(100L);
        movie.setTrailerUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        when(movieRepository.findByTmdbId(100L)).thenReturn(Optional.of(movie));

        TrailerMediaResponse response = movieTrailerService.resolveTrailerForTmdbId(100L);

        assertTrue(response.isAvailable());
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", response.getTrailerPlaybackUrl());
    }
}
