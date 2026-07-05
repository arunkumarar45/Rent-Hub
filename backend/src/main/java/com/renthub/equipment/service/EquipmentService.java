package com.renthub.equipment.service;

import com.renthub.common.dto.PageResponse;
import com.renthub.equipment.model.dto.EquipmentDto;
import com.renthub.equipment.model.dto.EquipmentRequest;

import java.time.LocalDate;
import java.util.List;

public interface EquipmentService {
    PageResponse<EquipmentDto> searchEquipment(
            String keyword, String location, String categorySlug,
            Integer minPrice, Integer maxPrice, LocalDate startDate, LocalDate endDate,
            int page, int size, String sortBy, String sortDir);

    EquipmentDto getEquipmentById(Long id);
    EquipmentDto createEquipment(EquipmentRequest request);
    EquipmentDto updateEquipment(Long id, EquipmentRequest request);
    void deleteEquipment(Long id);
    
    List<EquipmentDto> getMyEquipment();
    void blockDates(Long id, List<LocalDate> dates, String reason);
    void addSpecialPricing(Long id, LocalDate start, LocalDate end, int dailyRate, String reason);
    
    // Wishlist
    void toggleWishlist(Long id);
    List<EquipmentDto> getWishlist();
    
    List<EquipmentDto> getPopularEquipment();
}
