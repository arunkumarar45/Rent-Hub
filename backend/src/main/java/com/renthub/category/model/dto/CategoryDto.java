package com.renthub.category.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Flat category response DTO.
 * Denormalized parentId and parentName are included for convenience
 * so clients do not need a second request to resolve the parent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
    private Boolean isActive;

    /** Null when this is a root category. */
    private Long parentId;

    /** Null when this is a root category. */
    private String parentName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
