package com.bookmyshow.service;

import com.bookmyshow.dto.MovieDto;
import com.bookmyshow.dto.MovieRequest;
import com.bookmyshow.dto.MovieResponse;
import com.bookmyshow.dto.mapper.MovieMapper;
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
    private final MovieTrailerService movieTrailerService;

    public MovieService(MovieRepository movieRepository, MovieTrailerService movieTrailerService) {
        this.movieRepository = movieRepository;
        this.movieTrailerService = movieTrailerService;
    }

    @CacheEvict(value = "movies", allEntries = true)
    public MovieResponse addMovie(MovieRequest request) {
        log.info("Adding new movie: {}", request.getTitle());
        Movie saved = movieRepository.save(MovieMapper.toNewEntity(request));
        return toEnrichedResponse(saved);
    }

    @Deprecated
    @CacheEvict(value = "movies", allEntries = true)
    public MovieResponse addMovie(MovieDto dto) {
        return addMovie(MovieMapper.toRequest(dto));
    }

    @Caching(evict = {
        @CacheEvict(value = "movies", allEntries = true),
        @CacheEvict(value = "movieDetails", key = "#id")
    })
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        log.info("Updating movie id: {}", id);
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
        MovieMapper.applyUpdate(movie, request);
        return toEnrichedResponse(movieRepository.save(movie));
    }

    @Deprecated
    @Caching(evict = {
        @CacheEvict(value = "movies", allEntries = true),
        @CacheEvict(value = "movieDetails", key = "#id")
    })
    public MovieResponse updateMovie(Long id, MovieDto dto) {
        return updateMovie(id, MovieMapper.toRequest(dto));
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
    public List<MovieResponse> getAllMovies() {
        log.info("Cache Miss: Fetching active catalogue movies from MySQL database");
        return movieRepository.findByCatalogActiveTrue().stream()
                .map(this::toEnrichedResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "movieDetails", key = "#id")
    public MovieResponse getMovieById(Long id) {
        log.info("Cache Miss: Fetching movie details for id {} from MySQL database", id);
        return movieRepository.findById(id).map(this::toEnrichedResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
    }

    public MovieResponse getMovieByTmdbId(Long tmdbId) {
        return movieRepository.findByTmdbId(tmdbId).map(this::toEnrichedResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found for TMDB id " + tmdbId));
    }

    public List<MovieResponse> searchMoviesByTitle(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title)
            .stream().map(this::toEnrichedResponse).collect(Collectors.toList());
    }

    public Page<MovieResponse> getMoviesPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return movieRepository.findAll(pageable).map(this::toEnrichedResponse);
    }

    public Page<MovieResponse> searchMoviesPaginated(String query, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return movieRepository.findByTitleContainingIgnoreCaseOrGenreContainingIgnoreCaseOrLanguageIgnoreCase(query, query, query, pageable)
                .map(this::toEnrichedResponse);
    }

    private MovieResponse toEnrichedResponse(Movie movie) {
        MovieResponse response = MovieMapper.toResponse(movie);
        movieTrailerService.enrichMovieResponse(response, movie);
        return response;
    }
}
