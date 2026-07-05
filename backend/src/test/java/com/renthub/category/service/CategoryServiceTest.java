package com.renthub.category.service;

import com.renthub.category.mapper.CategoryMapper;
import com.renthub.category.model.dto.CategoryDto;
import com.renthub.category.model.dto.CategoryRequest;
import com.renthub.category.model.dto.CategoryTreeDto;
import com.renthub.category.model.entity.EquipmentCategory;
import com.renthub.category.repository.CategoryRepository;
import com.renthub.common.exception.BadRequestException;
import com.renthub.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private EquipmentCategory rootCategory;
    private EquipmentCategory childCategory;
    private CategoryDto rootCategoryDto;
    private CategoryDto childCategoryDto;

    @BeforeEach
    void setUp() {
        rootCategory = EquipmentCategory.builder()
                .id(1L)
                .name("Cameras & Photography")
                .slug("cameras-photography")
                .description("Professional and consumer cameras")
                .displayOrder(1)
                .isActive(true)
                .parent(null)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        childCategory = EquipmentCategory.builder()
                .id(2L)
                .name("DSLR Cameras")
                .slug("dslr-cameras")
                .displayOrder(1)
                .isActive(true)
                .parent(rootCategory)
                .children(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        rootCategoryDto = CategoryDto.builder()
                .id(1L)
                .name("Cameras & Photography")
                .slug("cameras-photography")
                .isActive(true)
                .build();

        childCategoryDto = CategoryDto.builder()
                .id(2L)
                .name("DSLR Cameras")
                .slug("dslr-cameras")
                .parentId(1L)
                .parentName("Cameras & Photography")
                .isActive(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createCategory tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createCategory: success creates root category with generated slug")
    void createCategory_Success_RootCategory() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Power Tools")
                .description("Electric and pneumatic tools")
                .displayOrder(2)
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Power Tools")).thenReturn(false);
        when(categoryRepository.findBySlug("power-tools")).thenReturn(Optional.empty());
        EquipmentCategory saved = EquipmentCategory.builder()
                .id(10L).name("Power Tools").slug("power-tools")
                .isActive(true).parent(null).children(new ArrayList<>()).build();
        when(categoryRepository.save(any(EquipmentCategory.class))).thenReturn(saved);
        CategoryDto expectedDto = CategoryDto.builder().id(10L).name("Power Tools").slug("power-tools").build();
        when(categoryMapper.toDto(saved)).thenReturn(expectedDto);

        CategoryDto result = categoryService.createCategory(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getSlug()).isEqualTo("power-tools");
        verify(categoryRepository, times(1)).save(any(EquipmentCategory.class));
    }

    @Test
    @DisplayName("createCategory: duplicate name throws BadRequestException")
    void createCategory_DuplicateName_ThrowsBadRequest() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Power Tools")
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Power Tools")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCategory: creating sub-category under another sub-category throws BadRequestException")
    void createCategory_SubCategoryUnderSubCategory_ThrowsBadRequest() {
        // grandChild has a parent (childCategory) that itself has a parent (rootCategory)
        CategoryRequest request = CategoryRequest.builder()
                .name("Canon DSLRs")
                .parentId(2L)
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Canon DSLRs")).thenReturn(false);
        when(categoryRepository.findBySlug("canon-dslrs")).thenReturn(Optional.empty());
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory));
        // childCategory.parent = rootCategory → two levels deep → should reject

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("one level of nesting");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCategory: parent not found throws ResourceNotFoundException")
    void createCategory_ParentNotFound_ThrowsResourceNotFound() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Mirrorless")
                .parentId(999L)
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Mirrorless")).thenReturn(false);
        when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCategory: slug collision resolved with numeric suffix")
    void createCategory_SlugCollision_GeneratesUniqueSuffix() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Power Tools")
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Power Tools")).thenReturn(false);
        // First call: "power-tools" is taken; second call: "power-tools-1" is free
        when(categoryRepository.findBySlug("power-tools")).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findBySlug("power-tools-1")).thenReturn(Optional.empty());

        EquipmentCategory saved = EquipmentCategory.builder()
                .id(11L).name("Power Tools").slug("power-tools-1")
                .isActive(true).parent(null).children(new ArrayList<>()).build();
        when(categoryRepository.save(any(EquipmentCategory.class))).thenReturn(saved);
        when(categoryMapper.toDto(saved)).thenReturn(
                CategoryDto.builder().id(11L).slug("power-tools-1").build());

        CategoryDto result = categoryService.createCategory(request);

        assertThat(result.getSlug()).isEqualTo("power-tools-1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateCategory tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateCategory: success updates fields")
    void updateCategory_Success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Photography Equipment")
                .description("Updated description")
                .displayOrder(3)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Photography Equipment", 1L)).thenReturn(false);
        // existsBySlugAndIdNot is only called when the name changes; use lenient to avoid UnnecessaryStubbing
        lenient().when(categoryRepository.existsBySlugAndIdNot(anyString(), eq(1L))).thenReturn(false);
        // countByParentId is only called when parentId is set; use lenient
        lenient().when(categoryRepository.countByParentId(1L)).thenReturn(0L);
        EquipmentCategory updated = EquipmentCategory.builder()
                .id(1L).name("Photography Equipment").slug("photography-equipment")
                .isActive(true).parent(null).children(new ArrayList<>()).build();
        when(categoryRepository.save(any(EquipmentCategory.class))).thenReturn(updated);
        when(categoryMapper.toDto(updated)).thenReturn(
                CategoryDto.builder().id(1L).name("Photography Equipment").slug("photography-equipment").build());

        CategoryDto result = categoryService.updateCategory(1L, request);

        assertThat(result.getName()).isEqualTo("Photography Equipment");
        verify(categoryRepository, times(1)).save(any(EquipmentCategory.class));
    }

    @Test
    @DisplayName("updateCategory: setting itself as parent throws BadRequestException")
    void updateCategory_SelfAsParent_ThrowsBadRequest() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Cameras & Photography")
                .parentId(1L) // same as the category being updated
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Cameras & Photography", 1L)).thenReturn(false);
        // The slug lookup may or may not be called depending on whether the name changed;
        // use lenient to avoid UnnecessaryStubbing when name is the same as existing
        lenient().when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be its own parent");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteCategory tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCategory: success when no children exist")
    void deleteCategory_Success_NoChildren() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.countByParentId(1L)).thenReturn(0L);

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).delete(rootCategory);
    }

    @Test
    @DisplayName("deleteCategory: throws BadRequestException when category has children")
    void deleteCategory_WithChildren_ThrowsBadRequest() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.countByParentId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("3 sub-category");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteCategory: category not found throws ResourceNotFoundException")
    void deleteCategory_NotFound_ThrowsResourceNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toggleCategoryStatus tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toggleCategoryStatus: deactivating parent cascades to children")
    void toggleStatus_DeactivatesParent_CascadesToChildren() {
        // rootCategory is currently active → toggling deactivates it
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findAllByParentIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(childCategory));
        when(categoryRepository.saveAll(any())).thenReturn(List.of(childCategory));
        when(categoryRepository.save(rootCategory)).thenReturn(rootCategory);
        when(categoryMapper.toDto(rootCategory)).thenReturn(
                CategoryDto.builder().id(1L).isActive(false).build());

        CategoryDto result = categoryService.toggleCategoryStatus(1L);

        assertThat(result.getIsActive()).isFalse();
        assertThat(childCategory.getIsActive()).isFalse(); // child was cascaded
        verify(categoryRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("toggleCategoryStatus: activating child when parent is inactive throws BadRequestException")
    void toggleStatus_ActivateChild_InactiveParent_ThrowsBadRequest() {
        rootCategory.setIsActive(false); // parent is inactive
        childCategory.setIsActive(false); // child is also inactive

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory));

        assertThatThrownBy(() -> categoryService.toggleCategoryStatus(2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("parent category");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // read operation tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCategoryBySlug: returns category when slug exists")
    void getCategoryBySlug_Success() {
        when(categoryRepository.findBySlug("cameras-photography"))
                .thenReturn(Optional.of(rootCategory));
        when(categoryMapper.toDto(rootCategory)).thenReturn(rootCategoryDto);

        CategoryDto result = categoryService.getCategoryBySlug("cameras-photography");

        assertThat(result.getSlug()).isEqualTo("cameras-photography");
    }

    @Test
    @DisplayName("getCategoryBySlug: slug not found throws ResourceNotFoundException")
    void getCategoryBySlug_NotFound_ThrowsResourceNotFound() {
        when(categoryRepository.findBySlug("nonexistent-slug")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryBySlug("nonexistent-slug"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    @DisplayName("getAllCategoriesAsTree: builds tree with children")
    void getAllCategoriesAsTree_ReturnsTreeWithChildren() {
        when(categoryRepository.findAllByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(rootCategory));
        when(categoryRepository.findAllByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(childCategory));
        when(categoryMapper.toDtoList(List.of(childCategory)))
                .thenReturn(List.of(childCategoryDto));

        List<CategoryTreeDto> tree = categoryService.getAllCategoriesAsTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("Cameras & Photography");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getName()).isEqualTo("DSLR Cameras");
    }

    @Test
    @DisplayName("getAllActiveCategories: returns flat list")
    void getAllActiveCategories_ReturnsFlatList() {
        when(categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(rootCategory, childCategory));
        when(categoryMapper.toDtoList(any())).thenReturn(List.of(rootCategoryDto, childCategoryDto));

        List<CategoryDto> result = categoryService.getAllActiveCategories();

        assertThat(result).hasSize(2);
    }
}
