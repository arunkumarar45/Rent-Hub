package com.renthub.dashboard.controller;

import com.renthub.common.dto.ApiResponse;
import com.renthub.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboards & Analytics", description = "Endpoints for retrieving business analytics, dashboard statistics, and system telemetry.")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/v1/owner/dashboard")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get Owner Dashboard metrics", description = "Returns active listing count, occupancy rates, earnings, and monthly charts. Requires OWNER role.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOwnerDashboard() {
        Map<String, Object> stats = dashboardService.getOwnerDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Owner dashboard stats retrieved successfully."));
    }

    @GetMapping("/api/v1/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Admin Dashboard metrics", description = "Returns system users count, transaction aggregates, and review moderation logs. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminDashboard() {
        Map<String, Object> stats = dashboardService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Admin dashboard stats retrieved successfully."));
    }
}
