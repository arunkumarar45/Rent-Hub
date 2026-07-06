package com.renthub.admin.controller;

import com.renthub.admin.dto.AdminUserDto;
import com.renthub.admin.dto.UpdateUserRoleRequest;
import com.renthub.admin.service.AdminService;
import com.renthub.booking.model.dto.BookingDto;
import com.renthub.common.dto.ApiResponse;
import com.renthub.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — System Management", description = "Admin-only endpoints for managing users, bookings, and generating system reports.")
public class AdminController {

    private final AdminService adminService;

    // ─────────────────────────────────────────────────────────────────────────
    // USER MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Returns a paginated list of all registered users with their roles and blocked status.")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminUserDto> result = adminService.getAllUsers(pageable);
        PageResponse<AdminUserDto> response = PageResponse.<AdminUserDto>builder()
                .content(result.getContent())
                .pageNumber(result.getNumber())
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response, "Users fetched successfully."));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Update user role", description = "Assigns a new role (CUSTOMER, OWNER, ADMIN) to a user.")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        AdminUserDto updated = adminService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok(ApiResponse.success(updated, "User role updated successfully."));
    }

    @PatchMapping("/users/{id}/block")
    @Operation(summary = "Block or unblock a user", description = "Toggles the blocked status of a user account.")
    public ResponseEntity<ApiResponse<AdminUserDto>> toggleUserBlock(@PathVariable Long id) {
        AdminUserDto updated = adminService.toggleUserBlock(id);
        String msg = Boolean.TRUE.equals(updated.getIsBlocked()) ? "User blocked successfully." : "User unblocked successfully.";
        return ResponseEntity.ok(ApiResponse.success(updated, msg));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOOKING MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/bookings")
    @Operation(summary = "List all bookings", description = "Returns all bookings across the entire system with pagination, newest first.")
    public ResponseEntity<ApiResponse<PageResponse<BookingDto>>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BookingDto> result = adminService.getAllBookings(pageable);
        PageResponse<BookingDto> response = PageResponse.<BookingDto>builder()
                .content(result.getContent())
                .pageNumber(result.getNumber())
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response, "All bookings fetched successfully."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REPORTS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/reports")
    @Operation(summary = "System report", description = "Returns a summary of user counts, booking stats, equipment count, and total platform revenue.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemReport() {
        Map<String, Object> report = adminService.getSystemReport();
        return ResponseEntity.ok(ApiResponse.success(report, "System report generated successfully."));
    }
}
