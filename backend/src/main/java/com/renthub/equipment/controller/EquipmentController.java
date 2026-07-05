package com.renthub.equipment.controller;

import com.renthub.common.dto.ApiResponse;
import com.renthub.common.dto.PageResponse;
import com.renthub.equipment.model.dto.EquipmentDto;
import com.renthub.equipment.model.dto.EquipmentRequest;
import com.renthub.equipment.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Equipment Catalog & Inventory", description = "Endpoints for searching items, viewing details, and owner listing management.")
public class EquipmentController {

    private final EquipmentService equipmentService;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ACCESS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/equipment")
    @Operation(summary = "Search equipment listings", description = "Query equipment with filters: keyword, location, category slug, price range, and calendar availability.")
    public ResponseEntity<ApiResponse<PageResponse<EquipmentDto>>> searchEquipment(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<EquipmentDto> results = equipmentService.searchEquipment(
                keyword, location, categorySlug, minPrice, maxPrice, startDate, endDate, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(results, "Equipment searched successfully."));
    }

    @GetMapping("/api/v1/equipment/{id}")
    @Operation(summary = "Get equipment details", description = "Retrieves complete details of an equipment listing by its ID.")
    public ResponseEntity<ApiResponse<EquipmentDto>> getEquipmentById(@PathVariable Long id) {
        EquipmentDto equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(ApiResponse.success(equipment, "Equipment details retrieved successfully."));
    }

    @GetMapping("/api/v1/equipment/popular")
    @Operation(summary = "Get popular equipment", description = "Returns top 8 newly listed, active equipment items.")
    public ResponseEntity<ApiResponse<List<EquipmentDto>>> getPopularEquipment() {
        List<EquipmentDto> popular = equipmentService.getPopularEquipment();
        return ResponseEntity.ok(ApiResponse.success(popular, "Popular equipment fetched successfully."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OWNER MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/owner/equipment")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List new equipment", description = "Creates a new equipment listing. Upgrades current user to OWNER if not already.")
    public ResponseEntity<ApiResponse<EquipmentDto>> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        EquipmentDto created = equipmentService.createEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Equipment listed successfully."));
    }

    @PutMapping("/api/v1/owner/equipment/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update equipment", description = "Updates an existing equipment listing. Requires owner validation.")
    public ResponseEntity<ApiResponse<EquipmentDto>> updateEquipment(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentRequest request) {
        EquipmentDto updated = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Equipment listing updated successfully."));
    }

    @DeleteMapping("/api/v1/owner/equipment/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete equipment", description = "Removes an equipment listing. Future search filters out deleted items.")
    public ResponseEntity<ApiResponse<Void>> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Equipment listing deleted successfully."));
    }

    @GetMapping("/api/v1/owner/equipment/my")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my listings", description = "Retrieves all equipment listings owned by the authenticated owner.")
    public ResponseEntity<ApiResponse<List<EquipmentDto>>> getMyEquipment() {
        List<EquipmentDto> myEquipment = equipmentService.getMyEquipment();
        return ResponseEntity.ok(ApiResponse.success(myEquipment, "My listings retrieved successfully."));
    }

    @PostMapping("/api/v1/owner/equipment/{id}/block-dates")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Block custom calendar dates", description = "Adds custom blocked dates to the availability calendar.")
    public ResponseEntity<ApiResponse<Void>> blockDates(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) List<LocalDate> dates,
            @RequestParam String reason) {
        equipmentService.blockDates(id, dates, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Availability calendar updated successfully."));
    }

    @PostMapping("/api/v1/owner/equipment/{id}/special-pricing")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add customized dynamic pricing rate", description = "Sets holiday or weekend surcharges / custom pricing schedules.")
    public ResponseEntity<ApiResponse<Void>> addSpecialPricing(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam int dailyRate,
            @RequestParam String reason) {
        equipmentService.addSpecialPricing(id, startDate, endDate, dailyRate, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Dynamic pricing rate schedule saved."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOMER WISHLIST
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/wishlist/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle wishlist item", description = "Adds/removes an equipment item to/from the user's wishlist.")
    public ResponseEntity<ApiResponse<Void>> toggleWishlist(@PathVariable Long id) {
        equipmentService.toggleWishlist(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Wishlist toggled successfully."));
    }

    @GetMapping("/api/v1/wishlist")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get wishlist items", description = "Retrieves all equipment items in the user's wishlist.")
    public ResponseEntity<ApiResponse<List<EquipmentDto>>> getWishlist() {
        List<EquipmentDto> wishlist = equipmentService.getWishlist();
        return ResponseEntity.ok(ApiResponse.success(wishlist, "Wishlist items fetched successfully."));
    }
}
