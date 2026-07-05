package com.renthub.equipment.repository;

import com.renthub.equipment.model.entity.EquipmentAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EquipmentAvailabilityRepository extends JpaRepository<EquipmentAvailability, Long> {
    List<EquipmentAvailability> findByEquipmentId(Long equipmentId);
    List<EquipmentAvailability> findByEquipmentIdAndBlockedDateBetween(Long equipmentId, LocalDate start, LocalDate end);
    void deleteByEquipmentId(Long equipmentId);
}
