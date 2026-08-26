package com.bookmyshow.service;

import com.bookmyshow.dto.MovieDto;
import com.bookmyshow.entity.Movie;
import com.bookmyshow.repository.MovieRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.bookmyshow.exception.ResourceNotFoundException;

@Slf4j
@Service
public class MovieService {
    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @CacheEvict(value = "movies", allEntries = true)
    public MovieDto addMovie(MovieDto dto) {
        log.info("Adding new movie: {}", dto.getTitle());
        Movie movie = new Movie();
        movie.setTitle(dto.getTitle());
        movie.setDescription(dto.getDescription());
        movie.setDuration(dto.getDuration());
        movie.setLanguage(dto.getLanguage());
        movie.setGenre(dto.getGenre());
        movie.setReleaseDate(dto.getReleaseDate());
        movie.setPosterPath(dto.getPosterPath());
        movie.setTrailerUrl(dto.getTrailerUrl());
        movie.setCast(dto.getCast());
        movie.setCertification(dto.getCertification());
        movie.setStatus(dto.getStatus());
        
        Movie saved = movieRepository.save(movie);
        return mapToDto(saved);
    }

    @Caching(evict = {
        @CacheEvict(value = "movies", allEntries = true),
        @CacheEvict(value = "movieDetails", key = "#id")
    })
    public MovieDto updateMovie(Long id, MovieDto dto) {
        log.info("Updating movie id: {}", id);
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
        movie.setTitle(dto.getTitle());
        movie.setDescription(dto.getDescription());
        movie.setDuration(dto.getDuration());
        movie.setLanguage(dto.getLanguage());
        movie.setGenre(dto.getGenre());
        movie.setReleaseDate(dto.getReleaseDate());
        if (dto.getPosterPath() != null) movie.setPosterPath(dto.getPosterPath());
        if (dto.getTrailerUrl() != null) movie.setTrailerUrl(dto.getTrailerUrl());
        if (dto.getCast() != null) movie.setCast(dto.getCast());
        if (dto.getCertification() != null) movie.setCertification(dto.getCertification());
        if (dto.getStatus() != null) movie.setStatus(dto.getStatus());

        Movie updated = movieRepository.save(movie);
        return mapToDto(updated);
    }

    @Caching(evict = {
        @CacheEvict(value = "movies", allEntries = true),
        @CacheEvict(value = "movieDetails", key = "#id")
    })
    public void deleteMovie(Long id) {
        log.info("Deleting movie id: {}", id);
        if (!movieRepository.existsById(id)) {
            throw new ResourceNotFoundException("Movie not found");
        }
        movieRepository.deleteById(id);
    }

    @Cacheable(value = "movies", key = "'all'")
    public List<MovieDto> getAllMovies() {
        log.info("Cache Miss: Fetching all movies from MySQL database");
        return movieRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Cacheable(value = "movieDetails", key = "#id")
    public MovieDto getMovieById(Long id) {
        log.info("Cache Miss: Fetching movie details for id {} from MySQL database", id);
        return movieRepository.findById(id).map(this::mapToDto)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
    }

    public List<MovieDto> searchMoviesByTitle(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title)
            .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public Page<MovieDto> getMoviesPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return movieRepository.findAll(pageable).map(this::mapToDto);
    }

    public Page<MovieDto> searchMoviesPaginated(String query, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return movieRepository.findByTitleContainingIgnoreCaseOrGenreContainingIgnoreCaseOrLanguageIgnoreCase(query, query, query, pageable)
                .map(this::mapToDto);
    }

    private MovieDto mapToDto(Movie movie) {
        MovieDto dto = new MovieDto();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setDescription(movie.getDescription());
        dto.setDuration(movie.getDuration());
        dto.setLanguage(movie.getLanguage());
        dto.setGenre(movie.getGenre());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setPosterPath(movie.getPosterPath());
        dto.setTrailerUrl(movie.getTrailerUrl());
        dto.setCast(movie.getCast());
        dto.setCertification(movie.getCertification());
        dto.setStatus(movie.getStatus());
        return dto;
    }
}
