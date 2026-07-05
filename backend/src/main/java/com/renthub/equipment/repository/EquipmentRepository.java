package com.renthub.equipment.repository;

import com.renthub.equipment.model.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {
    List<Equipment> findByOwnerId(Long ownerId);
    List<Equipment> findTop8ByIsActiveTrueOrderByCreatedAtDesc();
}
