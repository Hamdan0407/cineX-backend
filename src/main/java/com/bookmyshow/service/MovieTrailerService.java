package com.bookmyshow.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bookmyshow.config.CinexMediaProperties;
import com.bookmyshow.dto.TrailerMediaResponse;
import com.bookmyshow.entity.Movie;
import com.bookmyshow.repository.MovieRepository;

@Service
public class MovieTrailerService {

    private final MovieRepository movieRepository;
    private final CinexMediaProperties mediaProperties;
    private final MediaDeliveryService mediaDeliveryService;

    public MovieTrailerService(
            MovieRepository movieRepository,
            CinexMediaProperties mediaProperties,
            MediaDeliveryService mediaDeliveryService) {
        this.movieRepository = movieRepository;
        this.mediaProperties = mediaProperties;
        this.mediaDeliveryService = mediaDeliveryService;
    }

    public TrailerMediaResponse resolveTrailerForTmdbId(Long tmdbId) {
        if (tmdbId == null) {
            return TrailerMediaResponse.unavailable();
        }
        return resolveTrailerObjectKey(findTrailerObjectKey(tmdbId))
                .map(response -> {
                    response.setTmdbId(tmdbId);
                    return response;
                })
                .orElseGet(() -> {
                    TrailerMediaResponse unavailable = TrailerMediaResponse.unavailable();
                    unavailable.setTmdbId(tmdbId);
                    return unavailable;
                });
    }

    public Optional<TrailerMediaResponse> resolveTrailerForMovie(Movie movie) {
        if (movie == null) {
            return Optional.empty();
        }
        Long tmdbId = movie.getTmdbId();
        return findTrailerObjectKey(movie, tmdbId)
                .flatMap(key -> resolveTrailerObjectKey(Optional.of(key)))
                .map(response -> {
                    response.setTmdbId(tmdbId);
                    response.setBackendMovieId(movie.getId());
                    return response;
                });
    }

    public void enrichMovieResponse(com.bookmyshow.dto.MovieResponse response, Movie movie) {
        if (response == null || movie == null) {
            return;
        }
        resolveTrailerForMovie(movie).ifPresentOrElse(trailer -> {
            response.setTrailerObjectKey(trailer.getTrailerObjectKey());
            response.setTrailerPlaybackUrl(trailer.getTrailerPlaybackUrl());
        }, () -> {
            response.setTrailerObjectKey(null);
            response.setTrailerPlaybackUrl(null);
        });
    }

    public void enrichBookableMovieResponse(com.bookmyshow.dto.BookableMovieResponse response, Movie movie) {
        if (response == null || movie == null) {
            return;
        }
        resolveTrailerForMovie(movie).ifPresentOrElse(trailer -> {
            response.setTrailerObjectKey(trailer.getTrailerObjectKey());
            response.setTrailerPlaybackUrl(trailer.getTrailerPlaybackUrl());
        }, () -> {
            response.setTrailerObjectKey(null);
            response.setTrailerPlaybackUrl(null);
        });
    }

    private Optional<String> findTrailerObjectKey(Long tmdbId) {
        Optional<Movie> movie = movieRepository.findByTmdbId(tmdbId);
        return findTrailerObjectKey(movie.orElse(null), tmdbId);
    }

    private Optional<String> findTrailerObjectKey(Movie movie, Long tmdbId) {
        if (movie != null && StringUtils.hasText(movie.getTrailerUrl())) {
            return Optional.of(movie.getTrailerUrl().trim());
        }
        if (tmdbId != null && mediaProperties.getTrailerMappings() != null) {
            String mapped = mediaProperties.getTrailerMappings().get(tmdbId);
            if (StringUtils.hasText(mapped)) {
                return Optional.of(mapped.trim());
            }
        }
        return Optional.empty();
    }

    private Optional<TrailerMediaResponse> resolveTrailerObjectKey(Optional<String> objectKey) {
        if (objectKey.isEmpty()) {
            return Optional.empty();
        }
        String key = MediaDeliveryService.normalizeObjectKey(objectKey.get());
        if (!StringUtils.hasText(key)) {
            return Optional.empty();
        }
        String playbackUrl = mediaDeliveryService.resolveDeliveryUrl(key);
        if (!StringUtils.hasText(playbackUrl)) {
            return Optional.empty();
        }
        TrailerMediaResponse response = new TrailerMediaResponse();
        response.setTrailerObjectKey(key);
        response.setTrailerPlaybackUrl(playbackUrl);
        response.setAvailable(true);
        return Optional.of(response);
    }
}
