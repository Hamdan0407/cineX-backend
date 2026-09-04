package com.bookmyshow.controller;

import com.bookmyshow.dto.ShowRequest;
import com.bookmyshow.dto.ShowResponse;
import com.bookmyshow.dto.ShowSeatDto;
import com.bookmyshow.dto.BookableMovieResponse;
import com.bookmyshow.dto.CityShowAvailabilityResponse;
import com.bookmyshow.service.ShowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import org.springframework.data.domain.Page;

@Tag(name = "Show API", description = "Operations related to show api")
@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService showService;
    private final com.bookmyshow.service.AdminAuthService adminAuthService;

    public ShowController(ShowService showService, com.bookmyshow.service.AdminAuthService adminAuthService) {
        this.showService = showService;
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "Create show")
    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody ShowRequest request) {
        adminAuthService.validateAdmin();
        return new ResponseEntity<>(showService.addShow(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Update show")
    @PutMapping("/{id}")
    public ResponseEntity<ShowResponse> updateShow(@PathVariable Long id, @Valid @RequestBody ShowRequest request) {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(showService.updateShow(id, request));
    }

    @Operation(summary = "Delete show")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShow(@PathVariable Long id) {
        adminAuthService.validateAdmin();
        showService.deleteShow(id);
        return ResponseEntity.ok("Show deleted successfully");
    }

    @Operation(summary = "Get all shows", description = "Retrieves all scheduled shows across all theatres and movies.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved shows list")
    })
    @GetMapping
    public ResponseEntity<List<ShowResponse>> getAllShows() {
        return ResponseEntity.ok(showService.getAllShows());
    }

    @Operation(summary = "Get TMDB movie availability in a city")
    @GetMapping("/availability")
    public ResponseEntity<CityShowAvailabilityResponse> getCityAvailability(
            @RequestParam String city,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(showService.getCityAvailability(city, language));
    }

    @Operation(summary = "Get bookable movies with CineX show metadata for a city")
    @GetMapping("/bookable-movies")
    public ResponseEntity<List<BookableMovieResponse>> getBookableMovies(
            @RequestParam String city,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(showService.getBookableMovies(city, language));
    }

    @Operation(summary = "Get shows by TMDB movie ID for a city")
    @GetMapping("/tmdb/{tmdbId}")
    public ResponseEntity<List<ShowResponse>> getShowsByTmdbId(
            @PathVariable Long tmdbId,
            @RequestParam String city,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(showService.getShowsByTmdbId(tmdbId, city, language));
    }

    @Operation(summary = "Get shows by movie ID", description = "Fetches active shows for a specific movie.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved movie shows")
    })
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(
            @PathVariable Long movieId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(showService.getShowsByMovie(movieId, city, language));
    }

    @Operation(summary = "Get seating layout and availability for a show", description = "Returns seat tiers, prices, and blocked/booked status for a specific show ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved seating layout"),
        @ApiResponse(responseCode = "404", description = "Show not found")
    })
    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<ShowSeatDto>> getShowSeats(@PathVariable Long showId) {
        return ResponseEntity.ok(showService.getShowSeats(showId));
    }

    @Operation(summary = "Get paginated shows with size and sorting")
    @GetMapping("/paginated")
    public ResponseEntity<Page<ShowResponse>> getShowsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "showDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(showService.getShowsPaginated(page, size, sortBy, sortDir));
    }

    @Operation(summary = "Search paginated shows by movieId with size and sorting")
    @GetMapping("/search/paginated")
    public ResponseEntity<Page<ShowResponse>> searchShowsPaginated(
            @RequestParam(required = false) Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "showDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(showService.searchShowsPaginated(movieId, page, size, sortBy, sortDir));
    }
}
