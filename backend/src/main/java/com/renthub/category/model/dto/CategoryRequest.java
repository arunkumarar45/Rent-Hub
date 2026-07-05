package com.renthub.category.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared request DTO for both creating and updating an equipment category.
 * The slug is always auto-generated from the name — clients never supply it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 500, message = "Icon URL cannot exceed 500 characters")
    private String iconUrl;

    @Min(value = 0, message = "Display order must be 0 or greater")
    @Max(value = 9999, message = "Display order cannot exceed 9999")
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Optional: if supplied, this category becomes a subcategory of the given parent.
     * Must reference an existing root-level (parent-less) active category.
     * Null means this is a root category.
     */
    private Long parentId;
}
