package com.renthub.equipment.service;

import com.renthub.auth.model.entity.Role;
import com.renthub.auth.model.entity.RoleName;
import com.renthub.auth.model.entity.User;
import com.renthub.auth.repository.RoleRepository;
import com.renthub.auth.repository.UserRepository;
import com.renthub.category.model.entity.EquipmentCategory;
import com.renthub.category.repository.CategoryRepository;
import com.renthub.common.dto.PageResponse;
import com.renthub.common.exception.BadRequestException;
import com.renthub.common.exception.ForbiddenException;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.common.util.SecurityUtils;
import com.renthub.equipment.mapper.EquipmentMapper;
import com.renthub.equipment.model.dto.EquipmentDto;
import com.renthub.equipment.model.dto.EquipmentRequest;
import com.renthub.equipment.model.entity.Equipment;
import com.renthub.equipment.model.entity.EquipmentAvailability;
import com.renthub.equipment.model.entity.Favorite;
import com.renthub.equipment.model.entity.SpecialPricing;
import com.renthub.equipment.repository.EquipmentAvailabilityRepository;
import com.renthub.equipment.repository.EquipmentRepository;
import com.renthub.equipment.repository.EquipmentSpecification;
import com.renthub.equipment.repository.FavoriteRepository;
import com.renthub.equipment.repository.SpecialPricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentAvailabilityRepository availabilityRepository;
    private final SpecialPricingRepository specialPricingRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RoleRepository roleRepository;
    private final EquipmentMapper equipmentMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EquipmentDto> searchEquipment(
            String keyword, String location, String categorySlug,
            Integer minPrice, Integer maxPrice, LocalDate startDate, LocalDate endDate,
            int page, int size, String sortBy, String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Equipment> spec = Specification.where(EquipmentSpecification.isActive(true));

        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(EquipmentSpecification.hasKeyword(keyword));
        }
        if (location != null && !location.trim().isEmpty()) {
            spec = spec.and(EquipmentSpecification.hasLocation(location));
        }
        if (categorySlug != null && !categorySlug.trim().isEmpty()) {
            spec = spec.and(EquipmentSpecification.hasCategorySlug(categorySlug));
        }
        if (minPrice != null || maxPrice != null) {
            spec = spec.and(EquipmentSpecification.isPriceBetween(minPrice, maxPrice));
        }
        if (startDate != null && endDate != null) {
            LocalDateTime startLdt = startDate.atStartOfDay();
            LocalDateTime endLdt = endDate.atTime(LocalTime.MAX);
            spec = spec.and(EquipmentSpecification.isAvailable(startLdt, endLdt));
        }

        Page<Equipment> equipmentPage = equipmentRepository.findAll(spec, pageable);
        List<EquipmentDto> content = equipmentPage.getContent().stream()
                .map(equipmentMapper::toDto)
                .collect(Collectors.toList());

        return PageResponse.<EquipmentDto>builder()
                .content(content)
                .pageNumber(equipmentPage.getNumber())
                .pageSize(equipmentPage.getSize())
                .totalElements(equipmentPage.getTotalElements())
                .totalPages(equipmentPage.getTotalPages())
                .last(equipmentPage.isLast())
                .build();
    }

    @Override
    @Cacheable(value = "equipment", key = "#id")
    @Transactional(readOnly = true)
    public EquipmentDto getEquipmentById(Long id) {
        log.info("Fetching equipment by ID: {}", id);
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment", "id", id));
        return equipmentMapper.toDto(equipment);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"equipment", "popularEquipment"}, allEntries = true)
    public EquipmentDto createEquipment(EquipmentRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        EquipmentCategory category = categoryRepository.findBySlug(request.getCategorySlug())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", request.getCategorySlug()));

        // Check and elevate user to owner if not already
        if (!owner.getIsOwner()) {
            log.info("Elevating user ID {} to OWNER role", owner.getId());
            owner.setIsOwner(true);
            Role ownerRole = roleRepository.findByName(RoleName.ROLE_OWNER)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_OWNER).build()));
            if (owner.getRoles() == null) {
                owner.setRoles(new HashSet<>());
            }
            owner.addRole(ownerRole);
            userRepository.save(owner);
        }

        Equipment equipment = Equipment.builder()
                .owner(owner)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .dailyRate(request.getDailyRate())
                .deposit(request.getDeposit())
                .condition(request.getCondition())
                .location(request.getLocation())
                .imageUrl(request.getImageUrl())
                .images(request.getImages() != null ? request.getImages() : List.of())
                .isActive(true)
                .isDeleted(false)
                .build();

        Equipment saved = equipmentRepository.save(equipment);
        log.info("Successfully created equipment ID: {}", saved.getId());
        return equipmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"equipment", "popularEquipment"}, allEntries = true)
    public EquipmentDto updateEquipment(Long id, EquipmentRequest request) {
        Equipment equipment = getEquipmentAndVerifyOwnership(id);

        EquipmentCategory category = categoryRepository.findBySlug(request.getCategorySlug())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", request.getCategorySlug()));

        equipment.setCategory(category);
        equipment.setTitle(request.getTitle());
        equipment.setDescription(request.getDescription());
        equipment.setDailyRate(request.getDailyRate());
        equipment.setDeposit(request.getDeposit());
        equipment.setCondition(request.getCondition());
        equipment.setLocation(request.getLocation());
        if (request.getImageUrl() != null) {
            equipment.setImageUrl(request.getImageUrl());
        }
        if (request.getImages() != null) {
            equipment.setImages(request.getImages());
        }

        Equipment updated = equipmentRepository.save(equipment);
        log.info("Successfully updated equipment ID: {}", id);
        return equipmentMapper.toDto(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"equipment", "popularEquipment"}, allEntries = true)
    public void deleteEquipment(Long id) {
        Equipment equipment = getEquipmentAndVerifyOwnership(id);
        // Soft delete
        equipmentRepository.delete(equipment);
        log.info("Soft-deleted equipment ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentDto> getMyEquipment() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return equipmentRepository.findByOwnerId(user.getId()).stream()
                .map(equipmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void blockDates(Long id, List<LocalDate> dates, String reason) {
        Equipment equipment = getEquipmentAndVerifyOwnership(id);
        
        for (LocalDate date : dates) {
            EquipmentAvailability block = EquipmentAvailability.builder()
                    .equipment(equipment)
                    .blockedDate(date)
                    .reason(reason)
                    .build();
            availabilityRepository.save(block);
        }
        log.info("Blocked {} dates for equipment ID: {}", dates.size(), id);
    }

    @Override
    @Transactional
    public void addSpecialPricing(Long id, LocalDate start, LocalDate end, int dailyRate, String reason) {
        Equipment equipment = getEquipmentAndVerifyOwnership(id);
        if (start.isAfter(end)) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        SpecialPricing specialPricing = SpecialPricing.builder()
                .equipment(equipment)
                .startDate(start)
                .endDate(end)
                .dailyRate(dailyRate)
                .reason(reason)
                .build();
        specialPricingRepository.save(specialPricing);
        log.info("Added special pricing for equipment ID: {} from {} to {}", id, start, end);
    }

    @Override
    @Transactional
    public void toggleWishlist(Long id) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment", "id", id));

        favoriteRepository.findByUserIdAndEquipmentId(user.getId(), equipment.getId())
                .ifPresentOrElse(
                        favoriteRepository::delete,
                        () -> favoriteRepository.save(Favorite.builder().user(user).equipment(equipment).build())
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentDto> getWishlist() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return favoriteRepository.findByUserId(user.getId()).stream()
                .map(favorite -> equipmentMapper.toDto(favorite.getEquipment()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "popularEquipment")
    @Transactional(readOnly = true)
    public List<EquipmentDto> getPopularEquipment() {
        log.info("Fetching popular equipment listings from DB");
        return equipmentRepository.findTop8ByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(equipmentMapper::toDto)
                .collect(Collectors.toList());
    }

    private Equipment getEquipmentAndVerifyOwnership(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment", "id", id));
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        boolean isAdmin = SecurityUtils.getCurrentUserRoles().contains(RoleName.ROLE_ADMIN.name());
        if (!equipment.getOwner().getId().equals(user.getId()) && !isAdmin) {
            throw new ForbiddenException("You do not have permission to modify this equipment listing.");
        }
        return equipment;
    }
}
