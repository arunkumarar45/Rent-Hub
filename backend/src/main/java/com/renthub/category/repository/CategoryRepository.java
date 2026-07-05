package com.renthub.category.repository;

import com.renthub.category.model.entity.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<EquipmentCategory, Long> {

    /** All active categories ordered by display order (for flat public listing). */
    List<EquipmentCategory> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    /** Root-level active categories (parent is null) ordered by display order. */
    List<EquipmentCategory> findAllByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

    /** All root categories regardless of active status (admin use). */
    List<EquipmentCategory> findAllByParentIsNullOrderByDisplayOrderAsc();

    /** Lookup by slug for public URL-friendly endpoints. */
    Optional<EquipmentCategory> findBySlug(String slug);

    /** Check if a name already exists (case-insensitive) — for create uniqueness. */
    boolean existsByNameIgnoreCase(String name);

    /** Check name uniqueness while excluding the category being updated. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Check slug uniqueness while excluding the category being updated. */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /** Count children of a parent (used before deletion). */
    long countByParentId(Long parentId);

    /** Count active children of a parent. */
    long countByParentIdAndIsActiveTrue(Long parentId);

    /**
     * Find all direct children of a parent, ordered for display.
     * Used to cascade deactivation and for tree building.
     */
    List<EquipmentCategory> findAllByParentIdOrderByDisplayOrderAsc(Long parentId);

    /**
     * Find all active children of a parent ordered for display.
     */
    List<EquipmentCategory> findAllByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(Long parentId);

    /** Find all root categories with active flag for admin (all statuses). */
    @Query("SELECT c FROM EquipmentCategory c WHERE c.parent IS NULL ORDER BY c.displayOrder ASC")
    List<EquipmentCategory> findAllRootCategories();
}
