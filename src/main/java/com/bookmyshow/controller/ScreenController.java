package com.bookmyshow.controller;

import com.bookmyshow.dto.ScreenRequest;
import com.bookmyshow.dto.ScreenResponse;
import com.bookmyshow.service.ScreenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
@Tag(name = "Screen API", description = "Operations related to screen api")
@RestController
@RequestMapping("/api/screens")
public class ScreenController {
    
    private final ScreenService screenService;
    private final com.bookmyshow.service.AdminAuthService adminAuthService;

    public ScreenController(ScreenService screenService, com.bookmyshow.service.AdminAuthService adminAuthService) {
        this.screenService = screenService;
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "Create screen")
    @PostMapping
    public ResponseEntity<ScreenResponse> addScreen(@Valid @RequestBody ScreenRequest request) {
        adminAuthService.validateAdmin();
        return new ResponseEntity<>(screenService.addScreen(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Update screen")
    @PutMapping("/{id}")
    public ResponseEntity<ScreenResponse> updateScreen(@PathVariable Long id, @Valid @RequestBody ScreenRequest request) {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(screenService.updateScreen(id, request));
    }

    @Operation(summary = "Delete screen")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteScreen(@PathVariable Long id) {
        adminAuthService.validateAdmin();
        screenService.deleteScreen(id);
        return ResponseEntity.ok("Screen deleted successfully");
    }

    @Operation(summary = "Get all screens")
    @GetMapping
    public ResponseEntity<List<ScreenResponse>> getAllScreens() {
        return ResponseEntity.ok(screenService.getAllScreens());
    }

    @Operation(summary = "Get screens by theatre")
    @GetMapping("/theatre/{theatreId}")
    public ResponseEntity<List<ScreenResponse>> getScreensByTheatreId(@PathVariable Long theatreId) {
        return ResponseEntity.ok(screenService.getScreensByTheatreId(theatreId));
    }
}
