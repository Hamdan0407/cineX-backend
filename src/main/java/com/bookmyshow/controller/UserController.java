package com.bookmyshow.controller;

import com.bookmyshow.dto.AuthResponse;
import com.bookmyshow.dto.LoginRequest;
import com.bookmyshow.dto.RegisterRequest;
import com.bookmyshow.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import com.bookmyshow.entity.User;
import com.bookmyshow.service.AdminAuthService;

@Tag(name = "User API", description = "Operations related to user api")
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final AdminAuthService adminAuthService;

    public UserController(UserService userService, AdminAuthService adminAuthService) {
        this.userService = userService;
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Login user")
    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.loginUser(request));
    }

    @Operation(summary = "Admin login operation")
    @PostMapping("/auth/admin-login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.adminLoginUser(request));
    }

    @Operation(summary = "Get paginated users (Admin only)")
    @GetMapping("/users/paginated")
    public ResponseEntity<Page<com.bookmyshow.dto.UserResponse>> getUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(userService.getUsersPaginated(page, size));
    }
}
