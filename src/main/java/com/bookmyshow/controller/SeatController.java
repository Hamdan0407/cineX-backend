package com.bookmyshow.controller;

import com.bookmyshow.dto.SeatDto;
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
    public ResponseEntity<?> buildSeatLayout(@PathVariable Long screenId, @RequestBody SeatLayoutRequest request,
                                             @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                             @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        try {
            adminAuthService.validateAdmin(roleHeader, emailHeader);
            return new ResponseEntity<>(seatService.buildSeatLayout(screenId, request), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "@PostMapping operation for ResponseEntity<?>")
    @PostMapping("/screen/{screenId}")
    public ResponseEntity<?> bulkCreateSeats(@PathVariable Long screenId,
                                             @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                             @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        try {
            adminAuthService.validateAdmin(roleHeader, emailHeader);
            return new ResponseEntity<>(seatService.bulkCreateSeats(screenId), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Update single seat")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSeat(@PathVariable Long id, @Valid @RequestBody SeatDto dto,
                                        @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                        @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        try {
            adminAuthService.validateAdmin(roleHeader, emailHeader);
            return ResponseEntity.ok(seatService.updateSeat(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete single seat")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSeat(@PathVariable Long id,
                                        @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                        @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        seatService.deleteSeat(id);
        return ResponseEntity.ok("Seat deleted successfully");
    }

    @Operation(summary = "@GetMapping operation for ResponseEntity<List<SeatDto>>")
    @GetMapping("/screen/{screenId}")
    public ResponseEntity<List<SeatDto>> getSeatsByScreen(@PathVariable Long screenId) {
        return ResponseEntity.ok(seatService.getSeatsByScreen(screenId));
    }
}
