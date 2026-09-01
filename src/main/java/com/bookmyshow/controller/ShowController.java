package com.bookmyshow.controller;

import com.bookmyshow.dto.ShowDto;
import com.bookmyshow.dto.ShowSeatDto;
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

    @Operation(summary = "@PostMapping operation for ResponseEntity<?>")
    @PostMapping
    public ResponseEntity<?> createShow(@Valid @RequestBody ShowDto showDto,
                                        @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                        @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        try {
            adminAuthService.validateAdmin();
            return new ResponseEntity<>(showService.addShow(showDto), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Update show")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateShow(@PathVariable Long id, @Valid @RequestBody ShowDto showDto,
                                        @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                        @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        try {
            adminAuthService.validateAdmin();
            return ResponseEntity.ok(showService.updateShow(id, showDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete show")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShow(@PathVariable Long id,
                                        @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                        @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin();
        showService.deleteShow(id);
        return ResponseEntity.ok("Show deleted successfully");
    }

    @Operation(summary = "Get all shows", description = "Retrieves all scheduled shows across all theatres and movies.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved shows list")
    })
    @GetMapping
    public ResponseEntity<List<ShowDto>> getAllShows() {
        return ResponseEntity.ok(showService.getAllShows());
    }

    @Operation(summary = "Get shows by movie ID", description = "Fetches active shows for a specific movie.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved movie shows")
    })
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowDto>> getShowsByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(showService.getShowsByMovie(movieId));
    }

    @Operation(summary = "Get seating layout and availability for a show", description = "Returns seat tiers, prices, and blocked/booked status for a specific show ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved seating layout"),
        @ApiResponse(responseCode = "404", description = "Show not found")
    })
    @GetMapping("/{showId}/seats")
    public ResponseEntity<?> getShowSeats(@PathVariable Long showId) {
        try {
            return ResponseEntity.ok(showService.getShowSeats(showId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Get paginated shows with size and sorting")
    @GetMapping("/paginated")
    public ResponseEntity<Page<ShowDto>> getShowsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "showDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(showService.getShowsPaginated(page, size, sortBy, sortDir));
    }

    @Operation(summary = "Search paginated shows by movieId with size and sorting")
    @GetMapping("/search/paginated")
    public ResponseEntity<Page<ShowDto>> searchShowsPaginated(
            @RequestParam(required = false) Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "showDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(showService.searchShowsPaginated(movieId, page, size, sortBy, sortDir));
    }
}
