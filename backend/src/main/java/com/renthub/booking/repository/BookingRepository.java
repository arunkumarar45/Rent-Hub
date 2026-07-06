package com.renthub.booking.repository;

import com.renthub.booking.model.entity.Booking;
import com.renthub.booking.model.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerId(Long customerId);
    List<Booking> findByEquipmentOwnerId(Long ownerId);
    List<Booking> findByEquipmentIdAndStatusIn(Long equipmentId, List<BookingStatus> statuses);

    boolean existsByEquipmentIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
            Long equipmentId, List<BookingStatus> statuses, LocalDateTime end, LocalDateTime start);

    Page<Booking> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
