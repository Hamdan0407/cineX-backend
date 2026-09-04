package com.bookmyshow.dto.mapper;

import com.bookmyshow.dto.MovieRequest;
import com.bookmyshow.dto.MovieResponse;
import com.bookmyshow.entity.Movie;

public final class MovieMapper {

    private MovieMapper() {
    }

    public static Movie toNewEntity(MovieRequest request) {
        Movie movie = new Movie();
        applyUpdate(movie, request);
        return movie;
    }

    public static void applyUpdate(Movie movie, MovieRequest request) {
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setDuration(request.getDuration());
        movie.setLanguage(request.getLanguage());
        movie.setGenre(request.getGenre());
        movie.setReleaseDate(request.getReleaseDate());
        if (request.getPosterPath() != null) {
            movie.setPosterPath(request.getPosterPath());
        }
        if (request.getTrailerUrl() != null) {
            movie.setTrailerUrl(request.getTrailerUrl());
        }
        if (request.getCast() != null) {
            movie.setCast(request.getCast());
        }
        if (request.getCertification() != null) {
            movie.setCertification(request.getCertification());
        }
        if (request.getStatus() != null) {
            movie.setStatus(request.getStatus());
        }
        if (request.getTmdbId() != null) {
            movie.setTmdbId(request.getTmdbId());
        }
    }

    public static MovieResponse toResponse(Movie movie) {
        MovieResponse response = new MovieResponse();
        response.setId(movie.getId());
        response.setTitle(movie.getTitle());
        response.setDescription(movie.getDescription());
        response.setDuration(movie.getDuration());
        response.setLanguage(movie.getLanguage());
        response.setGenre(movie.getGenre());
        response.setReleaseDate(movie.getReleaseDate());
        response.setPosterPath(movie.getPosterPath());
        response.setTrailerUrl(movie.getTrailerUrl());
        response.setCast(movie.getCast());
        response.setCertification(movie.getCertification());
        response.setStatus(movie.getStatus());
        response.setTmdbId(movie.getTmdbId());
        return response;
    }

    public static MovieRequest toRequest(MovieResponse source) {
        if (source == null) {
            return null;
        }
        MovieRequest request = new MovieRequest();
        request.setTitle(source.getTitle());
        request.setDescription(source.getDescription());
        request.setDuration(source.getDuration());
        request.setLanguage(source.getLanguage());
        request.setGenre(source.getGenre());
        request.setReleaseDate(source.getReleaseDate());
        request.setPosterPath(source.getPosterPath());
        request.setTrailerUrl(source.getTrailerUrl());
        request.setCast(source.getCast());
        request.setCertification(source.getCertification());
        request.setStatus(source.getStatus());
        return request;
    }
}
