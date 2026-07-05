package com.renthub.equipment.service;

import com.renthub.auth.model.entity.User;
import com.renthub.auth.repository.RoleRepository;
import com.renthub.auth.repository.UserRepository;
import com.renthub.category.model.entity.EquipmentCategory;
import com.renthub.category.repository.CategoryRepository;
import com.renthub.common.dto.PageResponse;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.equipment.mapper.EquipmentMapper;
import com.renthub.equipment.model.dto.EquipmentDto;
import com.renthub.equipment.model.dto.EquipmentRequest;
import com.renthub.equipment.model.entity.Equipment;
import com.renthub.equipment.repository.EquipmentAvailabilityRepository;
import com.renthub.equipment.repository.EquipmentRepository;
import com.renthub.equipment.repository.FavoriteRepository;
import com.renthub.equipment.repository.SpecialPricingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EquipmentMapper equipmentMapper;

    @InjectMocks
    private EquipmentServiceImpl equipmentService;

    private User testUser;
    private EquipmentCategory testCategory;
    private Equipment testEquipment;
    private EquipmentDto testEquipmentDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("owner@example.com")
                .firstName("John")
                .lastName("Doe")
                .isOwner(true)
                .build();

        testCategory = EquipmentCategory.builder()
                .id(1L)
                .name("Cameras")
                .slug("cameras")
                .build();

        testEquipment = Equipment.builder()
                .id(1L)
                .owner(testUser)
                .category(testCategory)
                .title("Sony A7 III")
                .description("Professional camera")
                .dailyRate(5000)
                .deposit(10000)
                .condition("Good")
                .location("New York")
                .isActive(true)
                .isDeleted(false)
                .build();

        testEquipmentDto = EquipmentDto.builder()
                .id(1L)
                .title("Sony A7 III")
                .dailyRate(5000)
                .categorySlug("cameras")
                .build();
    }

    @Test
    void getEquipmentById_Success() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(equipmentMapper.toDto(testEquipment)).thenReturn(testEquipmentDto);

        EquipmentDto result = equipmentService.getEquipmentById(1L);

        assertNotNull(result);
        assertEquals("Sony A7 III", result.getTitle());
        verify(equipmentRepository, times(1)).findById(1L);
    }

    @Test
    void getEquipmentById_NotFound() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> equipmentService.getEquipmentById(99L));
    }

    @Test
    void searchEquipment_Success() {
        Page<Equipment> page = new PageImpl<>(List.of(testEquipment));
        when(equipmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(equipmentMapper.toDto(testEquipment)).thenReturn(testEquipmentDto);

        PageResponse<EquipmentDto> response = equipmentService.searchEquipment(
                "Sony", null, null, null, null, null, null, 0, 10, "createdAt", "desc");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Sony A7 III", response.getContent().get(0).getTitle());
    }
}
