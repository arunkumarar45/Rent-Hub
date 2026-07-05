package com.renthub.equipment.repository;

import com.renthub.equipment.model.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(Long userId);
    Optional<Favorite> findByUserIdAndEquipmentId(Long userId, Long equipmentId);
    boolean existsByUserIdAndEquipmentId(Long userId, Long equipmentId);
}
