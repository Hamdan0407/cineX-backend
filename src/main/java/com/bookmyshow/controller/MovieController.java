package com.bookmyshow.controller;

import com.bookmyshow.dto.MovieDto;
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

import jakarta.validation.Valid;
@Tag(name = "Movie API", description = "Operations related to movie api")
@RestController
@RequestMapping("/api/movies")
public class MovieController {
    
    private final MovieService movieService;
    private final com.bookmyshow.service.AdminAuthService adminAuthService;

    public MovieController(MovieService movieService, com.bookmyshow.service.AdminAuthService adminAuthService) {
        this.movieService = movieService;
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "@PostMapping operation for ResponseEntity<MovieDto>")
    @PostMapping
    public ResponseEntity<MovieDto> addMovie(@Valid @RequestBody MovieDto dto,
                                             @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                             @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        return new ResponseEntity<>(movieService.addMovie(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Update movie")
    @PutMapping("/{id}")
    public ResponseEntity<MovieDto> updateMovie(@PathVariable Long id, @Valid @RequestBody MovieDto dto,
                                                @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                                @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        return ResponseEntity.ok(movieService.updateMovie(id, dto));
    }

    @Operation(summary = "Delete movie")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(@PathVariable Long id,
                                         @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                         @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        movieService.deleteMovie(id);
        return ResponseEntity.ok("Movie deleted successfully");
    }

    @Operation(summary = "Get all active movies", description = "Retrieves the complete list of movies currently playing or available in MySQL/Redis cache.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved movies list")
    })
    @GetMapping
    public ResponseEntity<List<MovieDto>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @Operation(summary = "Get movie details by ID", description = "Fetches comprehensive details for a specific movie including genre, language, duration, and TMDB metadata.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully found movie details"),
        @ApiResponse(responseCode = "404", description = "Movie not found with given ID")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getMovieById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(movieService.getMovieById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Search movies by title substring", description = "Performs case-insensitive substring matching on movie titles.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully executed search")
    })
    @GetMapping("/search")
    public ResponseEntity<List<MovieDto>> searchMovies(@RequestParam String title) {
        return ResponseEntity.ok(movieService.searchMoviesByTitle(title));
    }

    @Operation(summary = "Get paginated movies with size and sorting")
    @GetMapping("/paginated")
    public ResponseEntity<Page<MovieDto>> getMoviesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(movieService.getMoviesPaginated(page, size, sortBy, sortDir));
    }

    @Operation(summary = "Search paginated movies with size and sorting")
    @GetMapping("/search/paginated")
    public ResponseEntity<Page<MovieDto>> searchMoviesPaginated(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(movieService.searchMoviesPaginated(query, page, size, sortBy, sortDir));
    }
}
