package com.renthub.category.service;

import com.renthub.category.mapper.CategoryMapper;
import com.renthub.category.model.dto.CategoryDto;
import com.renthub.category.model.dto.CategoryRequest;
import com.renthub.category.model.dto.CategoryTreeDto;
import com.renthub.category.model.entity.EquipmentCategory;
import com.renthub.category.repository.CategoryRepository;
import com.renthub.common.exception.BadRequestException;
import com.renthub.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // ───────────────────────────────────────────────────────────────────────────
    // WRITE OPERATIONS (ADMIN only – enforced at controller layer via @PreAuthorize)
    // ───────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CategoryDto createCategory(CategoryRequest request) {
        log.info("Admin creating category with name: '{}'", request.getName());

        // Business Rule: Name must be unique (case-insensitive)
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException(
                    "A category with name '" + request.getName() + "' already exists");
        }

        String slug = generateUniqueSlug(request.getName(), null);

        EquipmentCategory parent = resolveParent(request.getParentId());

        // Business Rule: Only one level of nesting allowed
        if (parent != null && parent.getParent() != null) {
            throw new BadRequestException(
                    "Sub-categories cannot be created under another sub-category. " +
                    "Only one level of nesting is supported.");
        }

        EquipmentCategory category = EquipmentCategory.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(true)
                .parent(parent)
                .build();

        category = categoryRepository.save(category);
        log.info("Category created with ID: {} and slug: '{}'", category.getId(), category.getSlug());
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        log.info("Admin updating category ID: {}", id);

        EquipmentCategory category = findCategoryById(id);

        // Business Rule: New name must not conflict with another category
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new BadRequestException(
                    "A category with name '" + request.getName() + "' already exists");
        }

        // Regenerate slug if name changed
        String newSlug = category.getSlug();
        if (!category.getName().equalsIgnoreCase(request.getName())) {
            newSlug = generateUniqueSlug(request.getName(), id);
        }

        EquipmentCategory parent = resolveParent(request.getParentId());

        // Business Rule: Cannot set itself as parent
        if (parent != null && parent.getId().equals(id)) {
            throw new BadRequestException("A category cannot be its own parent");
        }

        // Business Rule: Only one level of nesting
        if (parent != null && parent.getParent() != null) {
            throw new BadRequestException(
                    "Sub-categories cannot be nested under another sub-category");
        }

        // Business Rule: Cannot change a parent category into a child
        // (it would orphan its existing children)
        if (parent != null && categoryRepository.countByParentId(id) > 0) {
            throw new BadRequestException(
                    "Cannot make a parent category into a sub-category while it still has children. " +
                    "Please reassign or delete the children first.");
        }

        category.setName(request.getName().trim());
        category.setSlug(newSlug);
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setParent(parent);

        category = categoryRepository.save(category);
        log.info("Category ID: {} updated successfully", id);
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Admin deleting category ID: {}", id);

        EquipmentCategory category = findCategoryById(id);

        // Business Rule: Cannot delete if it has children (prevents orphans)
        long childCount = categoryRepository.countByParentId(id);
        if (childCount > 0) {
            throw new BadRequestException(
                    "Cannot delete category '" + category.getName() + "' because it has " +
                    childCount + " sub-category(s). Please delete or reassign them first.");
        }

        categoryRepository.delete(category);
        log.info("Category ID: {} deleted successfully", id);
    }

    @Override
    @Transactional
    public CategoryDto toggleCategoryStatus(Long id) {
        log.info("Admin toggling status for category ID: {}", id);

        EquipmentCategory category = findCategoryById(id);
        boolean newStatus = !category.getIsActive();

        category.setIsActive(newStatus);

        // Business Rule: Deactivating a parent cascades to deactivate all children
        if (!newStatus && category.getParent() == null) {
            List<EquipmentCategory> children =
                    categoryRepository.findAllByParentIdOrderByDisplayOrderAsc(id);
            children.forEach(child -> {
                child.setIsActive(false);
                log.info("Cascading deactivation to child category ID: {}", child.getId());
            });
            categoryRepository.saveAll(children);
        }

        // Business Rule: Cannot activate a child when its parent is inactive
        if (newStatus && category.getParent() != null && !category.getParent().getIsActive()) {
            throw new BadRequestException(
                    "Cannot activate sub-category '" + category.getName() +
                    "' because its parent category '" + category.getParent().getName() +
                    "' is inactive. Activate the parent first.");
        }

        category = categoryRepository.save(category);
        log.info("Category ID: {} status toggled to: {}", id, newStatus ? "ACTIVE" : "INACTIVE");
        return categoryMapper.toDto(category);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // READ OPERATIONS (Public – no auth required)
    // ───────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeDto> getAllCategoriesAsTree() {
        // Fetch all active root categories
        List<EquipmentCategory> rootCategories =
                categoryRepository.findAllByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

        return rootCategories.stream()
                .map(this::buildTreeDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllActiveCategories() {
        List<EquipmentCategory> categories =
                categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
        return categoryMapper.toDtoList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryBySlug(String slug) {
        EquipmentCategory category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        return categoryMapper.toDto(findCategoryById(id));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the parent entity from the optional parentId.
     * Validates that the parent exists.
     */
    private EquipmentCategory resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", parentId));
    }

    /**
     * Finds a category by ID or throws ResourceNotFoundException.
     */
    private EquipmentCategory findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    /**
     * Generates a URL-friendly slug from the category name.
     * If the generated slug collides with an existing one, appends a numeric suffix.
     *
     * @param name the category name
     * @param excludeId if not null, excludes this ID from the uniqueness check (for updates)
     * @return unique slug
     */
    private String generateUniqueSlug(String name, Long excludeId) {
        String baseSlug = name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")   // keep only alphanumeric, space, hyphen
                .replaceAll("[\\s-]+", "-")          // collapse spaces/hyphens
                .replaceAll("(^-|-$)", "");          // strip leading/trailing hyphens

        String slug = baseSlug;
        int suffix = 1;

        while (isSlugTaken(slug, excludeId)) {
            slug = baseSlug + "-" + suffix++;
        }

        return slug;
    }

    private boolean isSlugTaken(String slug, Long excludeId) {
        if (excludeId == null) {
            return categoryRepository.findBySlug(slug).isPresent();
        }
        return categoryRepository.existsBySlugAndIdNot(slug, excludeId);
    }

    /**
     * Builds a CategoryTreeDto from a root EquipmentCategory entity,
     * embedding its active children as flat CategoryDto entries.
     */
    private CategoryTreeDto buildTreeDto(EquipmentCategory root) {
        List<EquipmentCategory> activeChildren =
                categoryRepository.findAllByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(root.getId());

        return CategoryTreeDto.builder()
                .id(root.getId())
                .name(root.getName())
                .slug(root.getSlug())
                .description(root.getDescription())
                .iconUrl(root.getIconUrl())
                .displayOrder(root.getDisplayOrder())
                .isActive(root.getIsActive())
                .children(categoryMapper.toDtoList(activeChildren))
                .createdAt(root.getCreatedAt())
                .updatedAt(root.getUpdatedAt())
                .build();
    }
}
