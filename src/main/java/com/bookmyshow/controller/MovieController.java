package com.bookmyshow.controller;

import com.bookmyshow.dto.MovieRequest;
import com.bookmyshow.dto.MovieResponse;
import com.bookmyshow.service.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.data.domain.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.bookmyshow.service.TmdbCatalogSyncService;
import java.util.Map;
import com.bookmyshow.entity.Movie;

import jakarta.validation.Valid;
@Tag(name = "Movie API", description = "Operations related to movie api")
@RestController
@RequestMapping("/api/movies")
public class MovieController {
    
    private final MovieService movieService;
    private final com.bookmyshow.service.AdminAuthService adminAuthService;
    private final TmdbCatalogSyncService tmdbCatalogSyncService;

    public MovieController(MovieService movieService, com.bookmyshow.service.AdminAuthService adminAuthService, TmdbCatalogSyncService tmdbCatalogSyncService) {
        this.movieService = movieService;
        this.adminAuthService = adminAuthService;
        this.tmdbCatalogSyncService = tmdbCatalogSyncService;
    }

    @Operation(summary = "Sync movies with TMDB now playing catalog")
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncTmdbCatalog() {
        adminAuthService.validateAdmin();
        Map<Long, Movie> synced = tmdbCatalogSyncService.syncAndGetActiveMovies(60);
        return ResponseEntity.ok(Map.of("message", "TMDB catalog synced successfully", "activeMoviesCount", synced.size()));
    }

    @Operation(summary = "@PostMapping operation for ResponseEntity<MovieDto>")
    @PostMapping
    public ResponseEntity<MovieResponse> addMovie(@Valid @RequestBody MovieRequest request) {
        adminAuthService.validateAdmin();
        return new ResponseEntity<>(movieService.addMovie(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Update movie")
    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    @Operation(summary = "Delete movie")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(@PathVariable Long id) {
        adminAuthService.validateAdmin();
        movieService.deleteMovie(id);
        return ResponseEntity.ok("Movie deleted successfully");
    }

    @Operation(summary = "Get all active movies", description = "Retrieves the complete list of movies currently playing or available in MySQL/Redis cache.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved movies list")
    })
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/by-tmdb/{tmdbId}")
    public ResponseEntity<MovieResponse> getMovieByTmdbId(@PathVariable Long tmdbId) {
        return ResponseEntity.ok(movieService.getMovieByTmdbId(tmdbId));
    }

    @Operation(summary = "Get movie details by ID", description = "Fetches comprehensive details for a specific movie including genre, language, duration, and TMDB metadata.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully found movie details"),
        @ApiResponse(responseCode = "404", description = "Movie not found with given ID")
    })
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @Operation(summary = "Search movies by title substring", description = "Performs case-insensitive substring matching on movie titles.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully executed search")
    })
    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(@RequestParam String title) {
        return ResponseEntity.ok(movieService.searchMoviesByTitle(title));
    }

    @Operation(summary = "Get paginated movies with size and sorting")
    @GetMapping("/paginated")
    public ResponseEntity<Page<MovieResponse>> getMoviesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(movieService.getMoviesPaginated(page, size, sortBy, sortDir));
    }

    @Operation(summary = "Search paginated movies with size and sorting")
    @GetMapping("/search/paginated")
    public ResponseEntity<Page<MovieResponse>> searchMoviesPaginated(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(movieService.searchMoviesPaginated(query, page, size, sortBy, sortDir));
    }
}
