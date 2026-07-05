package com.renthub.category.repository;

import com.renthub.category.model.entity.EquipmentCategory;
import com.renthub.common.config.JpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-level integration tests using @DataJpaTest (in-memory H2 database).
 * Tests verifies that JPA queries work correctly against a real relational database.
 */
@DataJpaTest
@Import(JpaConfig.class)
@DisplayName("CategoryRepository Integration Tests")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private EquipmentCategory cameras;
    private EquipmentCategory powerTools;
    private EquipmentCategory dslrCameras; // child of cameras

    @BeforeEach
    void setUp() {
        cameras = entityManager.persistAndFlush(EquipmentCategory.builder()
                .name("Cameras & Photography")
                .slug("cameras-photography")
                .displayOrder(1)
                .isActive(true)
                .build());

        powerTools = entityManager.persistAndFlush(EquipmentCategory.builder()
                .name("Power Tools")
                .slug("power-tools")
                .displayOrder(2)
                .isActive(true)
                .build());

        dslrCameras = entityManager.persistAndFlush(EquipmentCategory.builder()
                .name("DSLR Cameras")
                .slug("dslr-cameras")
                .displayOrder(1)
                .isActive(true)
                .parent(cameras)
                .build());

        entityManager.clear();
    }

    @Test
    @DisplayName("findBySlug: returns category when slug exists")
    void findBySlug_ReturnsCategory() {
        Optional<EquipmentCategory> result = categoryRepository.findBySlug("cameras-photography");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Cameras & Photography");
    }

    @Test
    @DisplayName("findBySlug: returns empty when slug does not exist")
    void findBySlug_ReturnsEmpty_WhenNotFound() {
        Optional<EquipmentCategory> result = categoryRepository.findBySlug("nonexistent-slug");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByIsActiveTrueOrderByDisplayOrderAsc: returns only active categories in order")
    void findAllActiveOrdered_ReturnsCorrectOrder() {
        // Deactivate powerTools
        powerTools = entityManager.find(EquipmentCategory.class, powerTools.getId());
        powerTools.setIsActive(false);
        entityManager.persistAndFlush(powerTools);
        entityManager.clear();

        List<EquipmentCategory> result = categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();

        // Should return cameras (order=1) and dslrCameras (order=1), NOT powerTools
        assertThat(result).noneMatch(c -> c.getName().equals("Power Tools"));
        assertThat(result.stream().anyMatch(c -> c.getName().equals("Cameras & Photography"))).isTrue();
    }

    @Test
    @DisplayName("findAllByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc: returns only root active categories")
    void findAllRootActive_ReturnsOnlyRoots() {
        List<EquipmentCategory> roots =
                categoryRepository.findAllByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

        // dslrCameras has a parent, so should not appear
        assertThat(roots).hasSize(2);
        assertThat(roots.stream().noneMatch(c -> c.getName().equals("DSLR Cameras"))).isTrue();
    }

    @Test
    @DisplayName("existsByNameIgnoreCase: returns true when name exists (different case)")
    void existsByNameIgnoreCase_CaseInsensitive() {
        assertThat(categoryRepository.existsByNameIgnoreCase("cameras & photography")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("CAMERAS & PHOTOGRAPHY")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("Nonexistent")).isFalse();
    }

    @Test
    @DisplayName("existsByNameIgnoreCaseAndIdNot: excludes target ID")
    void existsByNameIgnoreCaseAndIdNot_ExcludesOwnId() {
        // Same name as cameras but different ID → should return false (for update uniqueness check)
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot(
                "cameras & photography", cameras.getId())).isFalse();

        // Same name but different ID → should return true
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot(
                "cameras & photography", powerTools.getId())).isTrue();
    }

    @Test
    @DisplayName("countByParentId: returns correct child count")
    void countByParentId_ReturnsCorrectCount() {
        long count = categoryRepository.countByParentId(cameras.getId());
        assertThat(count).isEqualTo(1L); // only dslrCameras

        long countForLeaf = categoryRepository.countByParentId(dslrCameras.getId());
        assertThat(countForLeaf).isEqualTo(0L);
    }

    @Test
    @DisplayName("findAllByParentIdOrderByDisplayOrderAsc: returns children of parent")
    void findChildrenByParentId_ReturnsChildren() {
        List<EquipmentCategory> children =
                categoryRepository.findAllByParentIdOrderByDisplayOrderAsc(cameras.getId());

        assertThat(children).hasSize(1);
        assertThat(children.get(0).getName()).isEqualTo("DSLR Cameras");
    }

    @Test
    @DisplayName("findAllByParentIdAndIsActiveTrueOrderByDisplayOrderAsc: returns only active children")
    void findActiveChildrenByParentId_ReturnsOnlyActive() {
        // Deactivate the child
        EquipmentCategory child = entityManager.find(EquipmentCategory.class, dslrCameras.getId());
        child.setIsActive(false);
        entityManager.persistAndFlush(child);
        entityManager.clear();

        List<EquipmentCategory> activeChildren =
                categoryRepository.findAllByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(cameras.getId());

        assertThat(activeChildren).isEmpty();
    }

    @Test
    @DisplayName("slug column has unique constraint")
    void slugColumn_HasUniqueConstraint() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            // Attempt to persist a duplicate slug
            entityManager.persistAndFlush(EquipmentCategory.builder()
                    .name("Another Cameras")
                    .slug("cameras-photography") // duplicate slug
                    .displayOrder(10)
                    .isActive(true)
                    .build());
        });
    }
}
