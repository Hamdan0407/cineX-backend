package com.bookmyshow.controller;

import com.bookmyshow.dto.AdminDashboardDto;
import com.bookmyshow.service.AdminAuthService;
import com.bookmyshow.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Dashboard API", description = "Operations for admin dashboard metrics and KPIs")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminAuthService adminAuthService;

    @Operation(summary = "Get admin dashboard KPI metrics and chart data")
    @GetMapping
    public ResponseEntity<AdminDashboardDto> getDashboardStats() {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }
}
