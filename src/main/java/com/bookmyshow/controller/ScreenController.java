package com.bookmyshow.controller;

import com.bookmyshow.dto.ScreenDto;
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

    @Operation(summary = "@PostMapping operation for ResponseEntity<?>")
    @PostMapping
    public ResponseEntity<?> addScreen(@Valid @RequestBody ScreenDto dto,
                                       @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                       @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        try {
            adminAuthService.validateAdmin(roleHeader, emailHeader);
            return new ResponseEntity<>(screenService.addScreen(dto), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Update screen")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateScreen(@PathVariable Long id, @Valid @RequestBody ScreenDto dto,
                                          @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                          @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        try {
            adminAuthService.validateAdmin(roleHeader, emailHeader);
            return ResponseEntity.ok(screenService.updateScreen(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete screen")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteScreen(@PathVariable Long id,
                                          @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                          @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        screenService.deleteScreen(id);
        return ResponseEntity.ok("Screen deleted successfully");
    }

    @Operation(summary = "Get all screens")
    @GetMapping
    public ResponseEntity<List<ScreenDto>> getAllScreens() {
        return ResponseEntity.ok(screenService.getAllScreens());
    }

    @Operation(summary = "@GetMapping operation for ResponseEntity<List<ScreenDto>>")
    @GetMapping("/theatre/{theatreId}")
    public ResponseEntity<List<ScreenDto>> getScreensByTheatreId(@PathVariable Long theatreId) {
        return ResponseEntity.ok(screenService.getScreensByTheatreId(theatreId));
    }
}
