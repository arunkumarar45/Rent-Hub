package com.renthub.category.controller;

import com.renthub.category.model.dto.CategoryDto;
import com.renthub.category.model.dto.CategoryRequest;
import com.renthub.category.model.dto.CategoryTreeDto;
import com.renthub.category.service.CategoryService;
import com.renthub.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Equipment Category REST Controller.
 *
 * Public READ endpoints (no auth required):
 *   GET  /api/v1/categories           → flat list of all active categories
 *   GET  /api/v1/categories/tree      → hierarchical tree with children
 *   GET  /api/v1/categories/{id}      → single category by ID
 *   GET  /api/v1/categories/slug/{slug} → single category by slug
 *
 * Admin WRITE endpoints (ROLE_ADMIN required):
 *   POST   /api/v1/admin/categories          → create category
 *   PUT    /api/v1/admin/categories/{id}     → update category
 *   DELETE /api/v1/admin/categories/{id}     → delete category
 *   PATCH  /api/v1/admin/categories/{id}/toggle → toggle active/inactive
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Equipment Categories", description = "Manage equipment taxonomy. Public read access; write operations require ADMIN role.")
public class CategoryController {

    private final CategoryService categoryService;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC READ ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/categories")
    @Operation(
            summary = "List all active categories",
            description = "Returns a flat list of all active equipment categories ordered by display order. No authentication required."
    )
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllActiveCategories() {
        List<CategoryDto> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories fetched successfully"));
    }

    @GetMapping("/api/v1/categories/tree")
    @Operation(
            summary = "Get category tree",
            description = "Returns root categories with their active subcategories nested inside. No authentication required."
    )
    public ResponseEntity<ApiResponse<List<CategoryTreeDto>>> getCategoryTree() {
        List<CategoryTreeDto> tree = categoryService.getAllCategoriesAsTree();
        return ResponseEntity.ok(ApiResponse.success(tree, "Category tree fetched successfully"));
    }

    @GetMapping("/api/v1/categories/{id}")
    @Operation(
            summary = "Get category by ID",
            description = "Returns a single category by its numeric ID. No authentication required."
    )
    public ResponseEntity<ApiResponse<CategoryDto>> getCategoryById(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id) {
        CategoryDto category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category, "Category fetched successfully"));
    }

    @GetMapping("/api/v1/categories/slug/{slug}")
    @Operation(
            summary = "Get category by slug",
            description = "Returns a single category by its URL-friendly slug (e.g., 'power-tools'). No authentication required."
    )
    public ResponseEntity<ApiResponse<CategoryDto>> getCategoryBySlug(
            @Parameter(description = "Category slug", example = "power-tools", required = true)
            @PathVariable String slug) {
        CategoryDto category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category, "Category fetched successfully"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN WRITE ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create equipment category",
            description = "Creates a new equipment category. Slug is auto-generated from name. Requires ADMIN role."
    )
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryDto created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Category created successfully"));
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update equipment category",
            description = "Updates an existing category. Name change regenerates the slug. Requires ADMIN role."
    )
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @Parameter(description = "Category ID to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryDto updated = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Category updated successfully"));
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Delete equipment category",
            description = "Permanently deletes a category. Fails if the category has sub-categories. Requires ADMIN role."
    )
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "Category ID to delete", required = true)
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }

    @PatchMapping("/api/v1/admin/categories/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Toggle category active status",
            description = "Activates or deactivates a category. Deactivating a parent cascades to all its children. Requires ADMIN role."
    )
    public ResponseEntity<ApiResponse<CategoryDto>> toggleCategoryStatus(
            @Parameter(description = "Category ID to toggle", required = true)
            @PathVariable Long id) {
        CategoryDto updated = categoryService.toggleCategoryStatus(id);
        String msg = updated.getIsActive() ? "Category activated successfully" : "Category deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(updated, msg));
    }
}
