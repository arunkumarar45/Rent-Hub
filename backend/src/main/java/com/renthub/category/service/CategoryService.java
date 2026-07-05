package com.renthub.category.service;

import com.renthub.category.model.dto.CategoryDto;
import com.renthub.category.model.dto.CategoryRequest;
import com.renthub.category.model.dto.CategoryTreeDto;

import java.util.List;

public interface CategoryService {

    /**
     * Creates a new equipment category.
     * Only ADMIN can invoke this. Slug is auto-generated from the name.
     *
     * @param request validated category data
     * @return the created category as a flat DTO
     */
    CategoryDto createCategory(CategoryRequest request);

    /**
     * Updates an existing category by ID.
     * Name change triggers slug regeneration.
     *
     * @param id      the category ID to update
     * @param request validated update data
     * @return the updated category as a flat DTO
     */
    CategoryDto updateCategory(Long id, CategoryRequest request);

    /**
     * Hard-deletes a category. Business rule: cannot delete if it has subcategories.
     * Equipment listing references will be validated in the Equipment module.
     *
     * @param id the category ID to delete
     */
    void deleteCategory(Long id);

    /**
     * Toggles the is_active flag of a category.
     * Deactivating a parent cascades to deactivate all its children.
     * Activating a child when parent is inactive throws BadRequestException.
     *
     * @param id the category ID to toggle
     * @return the updated category DTO
     */
    CategoryDto toggleCategoryStatus(Long id);

    /**
     * Returns all root categories with their active children embedded.
     * Used for the public /tree endpoint.
     *
     * @return list of CategoryTreeDto (root categories only with children inside)
     */
    List<CategoryTreeDto> getAllCategoriesAsTree();

    /**
     * Returns all active categories as a flat list, ordered by displayOrder.
     * Used for the public flat /categories endpoint.
     *
     * @return flat list of CategoryDto
     */
    List<CategoryDto> getAllActiveCategories();

    /**
     * Returns a single active category by its URL-friendly slug.
     *
     * @param slug the category slug
     * @return CategoryDto
     */
    CategoryDto getCategoryBySlug(String slug);

    /**
     * Returns a single category by its ID (admin may fetch inactive categories).
     *
     * @param id the category ID
     * @return CategoryDto
     */
    CategoryDto getCategoryById(Long id);
}
