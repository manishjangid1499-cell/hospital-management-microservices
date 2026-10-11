package com.genc.auth_service.controller;

import com.genc.auth_service.dto.*;
import com.genc.auth_service.model.Role;
import com.genc.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.genc.auth_service.util.JwtUtil jwtUtil;

    // Public: Login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // Public: Patient self-registration
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    // Public: Validate token
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestParam String token) {
        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token validation result", isValid));
    }

    // Public: Forgot Password
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<UserDTO>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        UserDTO user = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("A temporary password has been generated successfully.", user));
    }

    // Authenticated: Change Password (for first login password change or voluntary change)
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<UserDTO>> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String idStr = jwtUtil.getUserIdFromToken(token);
                if (idStr != null) {
                    userId = Long.parseLong(idStr);
                }
            } catch (Exception e) {
                // Token parse fallback failed
            }
        }

        if (userId == null) {
            throw new RuntimeException("User authentication ID is required");
        }
        UserDTO user = authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", user));
    }

    // Admin: Create user with specific role
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody RegisterRequest request) {
        UserDTO user = authService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", user));
    }

    // Admin: Get all users
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<UserDTO> users = authService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // Get user by ID
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        UserDTO user = authService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    // Get users by role
    @GetMapping("/users/role/{role}")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsersByRole(@PathVariable Role role) {
        List<UserDTO> users = authService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // Admin: Deactivate user
    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserDTO>> deactivateUser(@PathVariable Long id) {
        UserDTO user = authService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", user));
    }

    // Admin: Activate user
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<UserDTO>> activateUser(@PathVariable Long id) {
        UserDTO user = authService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User activated", user));
    }

    // Admin: Reset password for another user
    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<UserDTO>> resetPassword(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody AdminResetPasswordRequest request) {

        if (adminId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String idStr = jwtUtil.getUserIdFromToken(token);
                if (idStr != null) {
                    adminId = Long.parseLong(idStr);
                }
            } catch (Exception e) {
                // Token parse fallback
            }
        }

        if (adminId == null) {
            throw new RuntimeException("Admin authentication ID is required");
        }

        UserDTO user = authService.resetPassword(id, adminId, request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", user));
    }

    // Admin: Delete user permanently
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}

