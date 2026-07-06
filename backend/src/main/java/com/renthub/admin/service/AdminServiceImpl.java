package com.renthub.admin.service;

import com.renthub.admin.dto.AdminUserDto;
import com.renthub.auth.model.entity.Role;
import com.renthub.auth.model.entity.RoleName;
import com.renthub.auth.model.entity.User;
import com.renthub.auth.repository.RoleRepository;
import com.renthub.auth.repository.UserRepository;
import com.renthub.booking.mapper.BookingMapper;
import com.renthub.booking.model.dto.BookingDto;
import com.renthub.booking.model.entity.Booking;
import com.renthub.booking.model.entity.BookingStatus;
import com.renthub.booking.repository.BookingRepository;
import com.renthub.common.exception.BadRequestException;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.equipment.repository.EquipmentRepository;
import com.renthub.payment.model.entity.Transaction;
import com.renthub.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final TransactionRepository transactionRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserDto> getAllUsers(Pageable pageable) {
        log.info("Admin fetching all users, page={}", pageable.getPageNumber());
        return userRepository.findAll(pageable).map(this::toAdminUserDto);
    }

    @Override
    @Transactional
    public AdminUserDto updateUserRole(Long userId, String roleName) {
        log.info("Admin updating role for user ID: {} to {}", userId, roleName);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        RoleName targetRole;
        try {
            targetRole = RoleName.valueOf("ROLE_" + roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + roleName + ". Valid roles: CUSTOMER, OWNER, ADMIN");
        }

        Role role = roleRepository.findByName(targetRole)
                .orElseGet(() -> roleRepository.save(Role.builder().name(targetRole).build()));

        // Clear current roles and set new one (keep existing roles, just add the new one)
        user.getRoles().clear();
        user.addRole(role);

        // Sync isOwner flag
        user.setIsOwner(targetRole == RoleName.ROLE_OWNER || targetRole == RoleName.ROLE_ADMIN);

        user = userRepository.save(user);
        log.info("Updated role for user ID: {} to {}", userId, targetRole);
        return toAdminUserDto(user);
    }

    @Override
    @Transactional
    public AdminUserDto toggleUserBlock(Long userId) {
        log.info("Admin toggling block status for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean newStatus = !Boolean.TRUE.equals(user.getIsBlocked());
        user.setIsBlocked(newStatus);
        user = userRepository.save(user);

        log.info("User ID: {} is now {}", userId, newStatus ? "BLOCKED" : "UNBLOCKED");
        return toAdminUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingDto> getAllBookings(Pageable pageable) {
        log.info("Admin fetching all bookings, page={}", pageable.getPageNumber());
        return bookingRepository.findAllByOrderByCreatedAtDesc(pageable).map(bookingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemReport() {
        log.info("Admin generating system report");

        long totalUsers = userRepository.count();
        long totalOwners = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsOwner())).count();
        long totalEquipment = equipmentRepository.count();
        long totalBookings = bookingRepository.count();

        long completedBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long activeBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.ACTIVE).count();
        long pendingBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING).count();

        long totalRevenue = transactionRepository.findAll().stream()
                .filter(t -> "SUCCESS".equals(t.getStatus()))
                .mapToLong(Transaction::getAmount)
                .sum();

        // Monthly booking counts
        Map<String, Long> monthlyBookings = bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCreatedAt().getMonth().name().substring(0, 3),
                        Collectors.counting()
                ));

        Map<String, Object> report = new HashMap<>();
        report.put("totalUsers", totalUsers);
        report.put("totalOwners", totalOwners);
        report.put("totalCustomers", totalUsers - totalOwners);
        report.put("totalEquipment", totalEquipment);
        report.put("totalBookings", totalBookings);
        report.put("completedBookings", completedBookings);
        report.put("activeBookings", activeBookings);
        report.put("pendingBookings", pendingBookings);
        report.put("totalRevenueCents", totalRevenue);
        report.put("monthlyBookings", monthlyBookings);

        return report;
    }

    private AdminUserDto toAdminUserDto(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
        return AdminUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .isVerified(user.getIsVerified())
                .isOwner(user.getIsOwner())
                .isBlocked(Boolean.TRUE.equals(user.getIsBlocked()))
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
