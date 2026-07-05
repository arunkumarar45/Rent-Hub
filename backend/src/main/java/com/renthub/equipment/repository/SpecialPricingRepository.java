package com.renthub.equipment.repository;

import com.renthub.equipment.model.entity.SpecialPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SpecialPricingRepository extends JpaRepository<SpecialPricing, Long> {
    List<SpecialPricing> findByEquipmentId(Long equipmentId);
    List<SpecialPricing> findByEquipmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long equipmentId, LocalDate start, LocalDate end);
    void deleteByEquipmentId(Long equipmentId);
}
