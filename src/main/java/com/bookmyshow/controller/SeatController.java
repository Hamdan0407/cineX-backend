package com.bookmyshow.controller;

import com.bookmyshow.dto.SeatRequest;
import com.bookmyshow.dto.SeatResponse;
import com.bookmyshow.service.SeatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import com.bookmyshow.dto.SeatLayoutRequest;
import jakarta.validation.Valid;

@Tag(name = "Seat API", description = "Operations related to seat api")
@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;
    private final com.bookmyshow.service.AdminAuthService adminAuthService;

    public SeatController(SeatService seatService, com.bookmyshow.service.AdminAuthService adminAuthService) {
        this.seatService = seatService;
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "Build custom seat layout with categories and pricing")
    @PostMapping("/layout/{screenId}")
    public ResponseEntity<List<SeatResponse>> buildSeatLayout(@PathVariable Long screenId,
                                                              @Valid @RequestBody SeatLayoutRequest request) {
        adminAuthService.validateAdmin();
        return new ResponseEntity<>(seatService.buildSeatLayout(screenId, request), HttpStatus.CREATED);
    }

    @Operation(summary = "Bulk create seats for screen")
    @PostMapping("/screen/{screenId}")
    public ResponseEntity<List<SeatResponse>> bulkCreateSeats(@PathVariable Long screenId) {
        adminAuthService.validateAdmin();
        return new ResponseEntity<>(seatService.bulkCreateSeats(screenId), HttpStatus.CREATED);
    }

    @Operation(summary = "Update single seat")
    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(@PathVariable Long id, @Valid @RequestBody SeatRequest request) {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(seatService.updateSeat(id, request));
    }

    @Operation(summary = "Delete single seat")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSeat(@PathVariable Long id) {
        adminAuthService.validateAdmin();
        seatService.deleteSeat(id);
        return ResponseEntity.ok("Seat deleted successfully");
    }

    @Operation(summary = "Get seats by screen")
    @GetMapping("/screen/{screenId}")
    public ResponseEntity<List<SeatResponse>> getSeatsByScreen(@PathVariable Long screenId) {
        return ResponseEntity.ok(seatService.getSeatsByScreen(screenId));
    }
}
