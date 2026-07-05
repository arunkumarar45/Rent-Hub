package com.renthub.auth.controller;

import com.renthub.auth.model.dto.*;
import com.renthub.auth.service.AuthService;
import com.renthub.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile & Owner Onboarding", description = "Endpoints for profile management and becoming an equipment owner")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final AuthService authService;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile", description = "Retrieves authenticated user details and assigned roles")
    public ResponseEntity<ApiResponse<UserDto>> getProfile() {
        UserDto profile = authService.getCurrentProfile();
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile fetched successfully"));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update user profile", description = "Updates first name, last name, phone, and profile image")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserDto updated = authService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profile updated successfully"));
    }

    @PostMapping("/become-owner")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upgrade account to Equipment Owner", description = "Converts standard customer to an equipment owner with ROLE_OWNER")
    public ResponseEntity<ApiResponse<BecomeOwnerResponse>> becomeOwner(@Valid @RequestBody BecomeOwnerRequest request) {
        BecomeOwnerResponse upgraded = authService.becomeOwner(request);
        return ResponseEntity.ok(ApiResponse.success(upgraded, "Successfully upgraded account to Owner status"));
    }
}
