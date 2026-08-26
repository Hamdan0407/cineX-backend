package com.bookmyshow.controller;

import com.bookmyshow.dto.TheatreDto;
import com.bookmyshow.service.TheatreService;
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

@Tag(name = "Theatre API", description = "Operations related to theatre api")
@RestController
@RequestMapping("/api/theatres")
public class TheatreController {
    
    private final TheatreService theatreService;
    private final com.bookmyshow.service.AdminAuthService adminAuthService;

    public TheatreController(TheatreService theatreService, com.bookmyshow.service.AdminAuthService adminAuthService) {
        this.theatreService = theatreService;
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "@PostMapping operation for ResponseEntity<TheatreDto>")
    @PostMapping
    public ResponseEntity<TheatreDto> addTheatre(@Valid @RequestBody TheatreDto dto,
                                                 @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                                 @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        return new ResponseEntity<>(theatreService.addTheatre(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Update theatre")
    @PutMapping("/{id}")
    public ResponseEntity<TheatreDto> updateTheatre(@PathVariable Long id, @Valid @RequestBody TheatreDto dto,
                                                    @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                                    @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        return ResponseEntity.ok(theatreService.updateTheatre(id, dto));
    }

    @Operation(summary = "Delete theatre")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTheatre(@PathVariable Long id,
                                           @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                           @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        theatreService.deleteTheatre(id);
        return ResponseEntity.ok("Theatre deleted successfully");
    }

    @Operation(summary = "Get all theatres", description = "Retrieves a complete list of theatres across all cities from MySQL or Redis cache.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved theatres list")
    })
    @GetMapping
    public ResponseEntity<List<TheatreDto>> getAllTheatres() {
        return ResponseEntity.ok(theatreService.getAllTheatres());
    }

    @Operation(summary = "Get theatre by ID", description = "Fetches specific theatre details including address, screens, and city.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully found theatre"),
        @ApiResponse(responseCode = "404", description = "Theatre not found with given ID")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getTheatreById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(theatreService.getTheatreById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Get theatres by city", description = "Filters theatres by city name (case-insensitive).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved city theatres")
    })
    @GetMapping("/city/{city}")
    public ResponseEntity<List<TheatreDto>> getTheatresByCity(@PathVariable String city) {
        return ResponseEntity.ok(theatreService.getTheatresByCity(city));
    }

    @Operation(summary = "Get distinct list of cities where theatres are operational")
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getAllCities() {
        return ResponseEntity.ok(theatreService.getAllCities());
    }

    @Operation(summary = "Get paginated theatres with size and sorting")
    @GetMapping("/paginated")
    public ResponseEntity<Page<TheatreDto>> getTheatresPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(theatreService.getTheatresPaginated(page, size, sortBy, sortDir));
    }

    @Operation(summary = "Search paginated theatres with size and sorting")
    @GetMapping("/search/paginated")
    public ResponseEntity<Page<TheatreDto>> searchTheatresPaginated(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(theatreService.searchTheatresPaginated(query, page, size, sortBy, sortDir));
    }
}
