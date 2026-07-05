package com.renthub.dashboard.service;

import com.renthub.auth.model.entity.User;
import com.renthub.auth.repository.UserRepository;
import com.renthub.booking.model.entity.Booking;
import com.renthub.booking.model.entity.BookingStatus;
import com.renthub.booking.repository.BookingRepository;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.common.util.SecurityUtils;
import com.renthub.equipment.model.entity.Equipment;
import com.renthub.equipment.repository.EquipmentRepository;
import com.renthub.payment.model.entity.Transaction;
import com.renthub.payment.repository.TransactionRepository;
import com.renthub.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;
    private final TransactionRepository transactionRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardData", key = "'owner-' + T(com.renthub.common.util.SecurityUtils).getCurrentUserEmail()")
    public Map<String, Object> getOwnerDashboardStats() {
        String email = SecurityUtils.getCurrentUserEmail();
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        log.info("Calculating owner dashboard stats for user ID: {}", owner.getId());

        List<Equipment> listings = equipmentRepository.findByOwnerId(owner.getId());
        List<Booking> bookings = bookingRepository.findByEquipmentOwnerId(owner.getId());

        long activeListingsCount = listings.stream().filter(Equipment::getIsActive).count();
        long totalBookingsCount = bookings.size();

        long totalEarnings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.ACTIVE || b.getStatus() == BookingStatus.COMPLETED)
                .mapToLong(Booking::getRentalPrice)
                .sum();

        // Calculate simple occupancy rate: % of approved/active rentals
        double occupancyRate = listings.isEmpty() ? 0.0 :
                (bookings.stream().filter(b -> b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.ACTIVE).count() / (double) listings.size()) * 100;
        occupancyRate = Math.min(100.0, Math.round(occupancyRate * 10.0) / 10.0);

        List<Booking> pendingRequests = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .collect(Collectors.toList());

        // Group monthly revenue
        Map<String, Long> monthlyRevenue = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.ACTIVE || b.getStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        b -> b.getStartDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        Collectors.summingLong(Booking::getRentalPrice)
                ));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEarnings", totalEarnings); // in cents
        stats.put("activeListingsCount", activeListingsCount);
        stats.put("totalBookingsCount", totalBookingsCount);
        stats.put("occupancyRate", occupancyRate);
        stats.put("pendingRequestsCount", pendingRequests.size());
        stats.put("monthlyRevenue", monthlyRevenue);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardData", key = "'admin'")
    public Map<String, Object> getAdminDashboardStats() {
        log.info("Calculating admin dashboard stats");

        long totalUsers = userRepository.count();
        long totalEquipment = equipmentRepository.count();
        long totalBookings = bookingRepository.count();

        long totalTransactions = transactionRepository.findAll().stream()
                .filter(t -> "SUCCESS".equals(t.getStatus()))
                .mapToLong(Transaction::getAmount)
                .sum();

        long pendingModerations = reviewRepository.findByIsModeratedFalse().size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalEquipment", totalEquipment);
        stats.put("totalBookings", totalBookings);
        stats.put("totalTransactions", totalTransactions); // in cents
        stats.put("pendingModerations", pendingModerations);

        return stats;
    }
}
