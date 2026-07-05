package com.renthub.category.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hierarchical category response DTO.
 * Used for the /tree endpoint to return root categories with their children embedded.
 * Only one level of nesting is supported — children do not themselves have a children list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeDto {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
    private Boolean isActive;

    /**
     * Active subcategories under this root category, ordered by displayOrder.
     * Empty list when there are no children.
     */
    @Builder.Default
    private List<CategoryDto> children = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
