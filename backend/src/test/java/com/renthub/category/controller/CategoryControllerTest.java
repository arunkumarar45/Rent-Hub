package com.renthub.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renthub.auth.service.CustomUserDetailsService;
import com.renthub.category.model.dto.CategoryDto;
import com.renthub.category.model.dto.CategoryRequest;
import com.renthub.category.model.dto.CategoryTreeDto;
import com.renthub.category.service.CategoryService;
import com.renthub.common.config.JwtAuthenticationFilter;
import com.renthub.common.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CategoryController Integration Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    // Security beans required for context to load
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // ─────────────────────────────────────────────────────────────────────────
    // Public READ endpoint tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/categories → 200 with flat list")
    void getAllActiveCategories_Returns200() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L).name("Power Tools").slug("power-tools").isActive(true).build();

        when(categoryService.getAllActiveCategories()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Categories fetched successfully"))
                .andExpect(jsonPath("$.data[0].name").value("Power Tools"))
                .andExpect(jsonPath("$.data[0].slug").value("power-tools"));
    }

    @Test
    @DisplayName("GET /api/v1/categories/tree → 200 with tree")
    void getCategoryTree_Returns200() throws Exception {
        CategoryDto child = CategoryDto.builder()
                .id(2L).name("DSLR Cameras").slug("dslr-cameras").parentId(1L).build();
        CategoryTreeDto tree = CategoryTreeDto.builder()
                .id(1L).name("Cameras").slug("cameras").isActive(true)
                .children(List.of(child)).build();

        when(categoryService.getAllCategoriesAsTree()).thenReturn(List.of(tree));

        mockMvc.perform(get("/api/v1/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Cameras"))
                .andExpect(jsonPath("$.data[0].children[0].name").value("DSLR Cameras"));
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} → 200 with category")
    void getCategoryById_Returns200() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L).name("Power Tools").slug("power-tools").build();

        when(categoryService.getCategoryById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Power Tools"));
    }

    @Test
    @DisplayName("GET /api/v1/categories/slug/{slug} → 200 with category")
    void getCategoryBySlug_Returns200() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L).name("Power Tools").slug("power-tools").build();

        when(categoryService.getCategoryBySlug("power-tools")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/categories/slug/power-tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("power-tools"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin WRITE endpoint tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/admin/categories → 201 when ADMIN")
    @WithMockUser(roles = "ADMIN")
    void createCategory_AsAdmin_Returns201() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Audio Equipment")
                .description("Microphones and speakers")
                .displayOrder(5)
                .build();

        CategoryDto created = CategoryDto.builder()
                .id(3L).name("Audio Equipment").slug("audio-equipment").isActive(true).build();

        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.data.name").value("Audio Equipment"))
                .andExpect(jsonPath("$.data.slug").value("audio-equipment"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/categories → 400 when name is blank")
    @WithMockUser(roles = "ADMIN")
    void createCategory_BlankName_Returns400() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("") // blank name – violates @NotBlank
                .build();

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/categories/{id} → 200 when ADMIN")
    @WithMockUser(roles = "ADMIN")
    void updateCategory_AsAdmin_Returns200() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Updated Power Tools")
                .displayOrder(1)
                .build();

        CategoryDto updated = CategoryDto.builder()
                .id(1L).name("Updated Power Tools").slug("updated-power-tools").isActive(true).build();

        when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Power Tools"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/categories/{id} → 200 when ADMIN")
    @WithMockUser(roles = "ADMIN")
    void deleteCategory_AsAdmin_Returns200() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));

        verify(categoryService, times(1)).deleteCategory(1L);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/categories/{id}/toggle → 200 toggles status")
    @WithMockUser(roles = "ADMIN")
    void toggleCategoryStatus_AsAdmin_Returns200() throws Exception {
        CategoryDto deactivated = CategoryDto.builder()
                .id(1L).name("Power Tools").slug("power-tools").isActive(false).build();

        when(categoryService.toggleCategoryStatus(1L)).thenReturn(deactivated);

        mockMvc.perform(patch("/api/v1/admin/categories/1/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.message").value("Category deactivated successfully"));
    }

    @Test
    @DisplayName("Verify that all Admin endpoints have proper PreAuthorize annotations")
    void verifyAdminEndpointsSecurity() throws Exception {
        // 1. createCategory
        java.lang.reflect.Method createMethod = CategoryController.class.getMethod("createCategory", CategoryRequest.class);
        org.springframework.security.access.prepost.PreAuthorize preAuthCreate = createMethod.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.assertj.core.api.Assertions.assertThat(preAuthCreate).isNotNull();
        org.assertj.core.api.Assertions.assertThat(preAuthCreate.value()).isEqualTo("hasRole('ADMIN')");

        // 2. updateCategory
        java.lang.reflect.Method updateMethod = CategoryController.class.getMethod("updateCategory", Long.class, CategoryRequest.class);
        org.springframework.security.access.prepost.PreAuthorize preAuthUpdate = updateMethod.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.assertj.core.api.Assertions.assertThat(preAuthUpdate).isNotNull();
        org.assertj.core.api.Assertions.assertThat(preAuthUpdate.value()).isEqualTo("hasRole('ADMIN')");

        // 3. deleteCategory
        java.lang.reflect.Method deleteMethod = CategoryController.class.getMethod("deleteCategory", Long.class);
        org.springframework.security.access.prepost.PreAuthorize preAuthDelete = deleteMethod.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.assertj.core.api.Assertions.assertThat(preAuthDelete).isNotNull();
        org.assertj.core.api.Assertions.assertThat(preAuthDelete.value()).isEqualTo("hasRole('ADMIN')");

        // 4. toggleCategoryStatus
        java.lang.reflect.Method toggleMethod = CategoryController.class.getMethod("toggleCategoryStatus", Long.class);
        org.springframework.security.access.prepost.PreAuthorize preAuthToggle = toggleMethod.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.assertj.core.api.Assertions.assertThat(preAuthToggle).isNotNull();
        org.assertj.core.api.Assertions.assertThat(preAuthToggle.value()).isEqualTo("hasRole('ADMIN')");
    }
}
