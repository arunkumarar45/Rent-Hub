package com.renthub.booking.service;

import com.renthub.auth.model.entity.User;
import com.renthub.auth.model.entity.RoleName;
import com.renthub.auth.repository.UserRepository;
import com.renthub.booking.mapper.BookingMapper;
import com.renthub.booking.model.dto.BookingDto;
import com.renthub.booking.model.dto.BookingRequest;
import com.renthub.booking.model.dto.PriceCalculationResponse;
import com.renthub.booking.model.entity.Booking;
import com.renthub.booking.model.entity.BookingStatus;
import com.renthub.booking.repository.BookingRepository;
import com.renthub.common.exception.BadRequestException;
import com.renthub.common.exception.ForbiddenException;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.common.util.SecurityUtils;
import com.renthub.coupon.model.entity.Coupon;
import com.renthub.coupon.service.CouponService;
import com.renthub.equipment.model.entity.Equipment;
import com.renthub.equipment.model.entity.SpecialPricing;
import com.renthub.equipment.repository.EquipmentAvailabilityRepository;
import com.renthub.equipment.repository.EquipmentRepository;
import com.renthub.equipment.repository.SpecialPricingRepository;
import com.renthub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentAvailabilityRepository availabilityRepository;
    private final SpecialPricingRepository specialPricingRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final InvoiceService invoiceService;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingDto createBooking(BookingRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment", "id", request.getEquipmentId()));

        if (equipment.getOwner().getId().equals(customer.getId())) {
            throw new BadRequestException("You cannot rent your own equipment listing.");
        }

        if (request.getStartDate().isAfter(request.getEndDate()) || request.getStartDate().isEqual(request.getEndDate())) {
            throw new BadRequestException("Start date must be before end date.");
        }

        // 1. Conflict Detection: check overlapping active bookings
        List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.ACTIVE);
        boolean isOverlapping = bookingRepository.existsByEquipmentIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
                equipment.getId(), activeStatuses, request.getEndDate(), request.getStartDate());
        if (isOverlapping) {
            throw new BadRequestException("Equipment is already reserved during the requested dates.");
        }

        // 2. Availability Calendar Check: check owner blocked dates
        LocalDate startLocalDate = request.getStartDate().toLocalDate();
        LocalDate endLocalDate = request.getEndDate().toLocalDate();
        long blockedCount = availabilityRepository.findByEquipmentIdAndBlockedDateBetween(
                equipment.getId(), startLocalDate, endLocalDate).size();
        if (blockedCount > 0) {
            throw new BadRequestException("Some dates in the requested range are blocked for maintenance.");
        }

        // 3. Price Calculation (supporting Dynamic Pricing & Special Surcharges)
        long rentalDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (rentalDays <= 0) rentalDays = 1;

        int rentalCost = 0;
        for (int i = 0; i < rentalDays; i++) {
            LocalDate currentDay = startLocalDate.plusDays(i);
            // Check special pricing rules
            List<SpecialPricing> specials = specialPricingRepository
                    .findByEquipmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            equipment.getId(), currentDay, currentDay);
            
            if (!specials.isEmpty()) {
                // Apply the first matching special pricing surcharge rate
                rentalCost += specials.get(0).getDailyRate();
            } else {
                rentalCost += equipment.getDailyRate();
            }
        }

        // Apply coupon code if provided
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            Coupon coupon = couponService.validateAndGetCoupon(request.getCouponCode(), rentalCost);
            if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
                rentalCost -= (rentalCost * coupon.getValue()) / 100;
            } else {
                rentalCost -= coupon.getValue();
            }
            if (rentalCost < 0) rentalCost = 0;
            couponService.incrementUses(coupon.getId());
        }

        int totalCost = rentalCost + equipment.getDeposit();

        Booking booking = Booking.builder()
                .equipment(equipment)
                .customer(customer)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .dailyRate(equipment.getDailyRate())
                .deposit(equipment.getDeposit())
                .rentalPrice(rentalCost)
                .totalPrice(totalCost)
                .status(BookingStatus.PENDING)
                .qrCode(UUID.randomUUID().toString())
                .build();

        booking = bookingRepository.save(booking);

        // Generate Invoice PDF
        String invoiceUrl = invoiceService.generateInvoicePdf(booking);
        booking.setInvoiceUrl(invoiceUrl);
        booking = bookingRepository.save(booking);

        // Notifications
        notificationService.createInAppNotification(equipment.getOwner().getId(),
                "New Booking Request",
                String.format("You have a new rental request for '%s' by %s.", equipment.getTitle(), customer.getFirstName()));
        
        notificationService.sendBookingConfirmationEmail(customer.getEmail(),
                customer.getFirstName(), equipment.getTitle(),
                request.getStartDate().toLocalDate() + " to " + request.getEndDate().toLocalDate(),
                totalCost / 100.0);

        log.info("Saved booking ID: {} with price: {} and deposit: {}", booking.getId(), rentalCost, equipment.getDeposit());
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceCalculationResponse calculatePrice(BookingRequest request) {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment", "id", request.getEquipmentId()));

        if (request.getStartDate().isAfter(request.getEndDate()) || request.getStartDate().isEqual(request.getEndDate())) {
            throw new BadRequestException("Start date must be before end date.");
        }

        LocalDate startLocalDate = request.getStartDate().toLocalDate();
        LocalDate endLocalDate = request.getEndDate().toLocalDate();

        long rentalDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (rentalDays <= 0) rentalDays = 1;

        int rentalCost = 0;
        for (int i = 0; i < rentalDays; i++) {
            LocalDate currentDay = startLocalDate.plusDays(i);
            
            List<SpecialPricing> specials = specialPricingRepository
                    .findByEquipmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            equipment.getId(), currentDay, currentDay);
            
            if (!specials.isEmpty()) {
                rentalCost += specials.get(0).getDailyRate();
            } else {
                rentalCost += equipment.getDailyRate();
            }
        }

        int discountAmount = 0;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            try {
                Coupon coupon = couponService.validateAndGetCoupon(request.getCouponCode(), rentalCost);
                int discountedCost = rentalCost;
                if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
                    discountedCost -= (rentalCost * coupon.getValue()) / 100;
                } else {
                    discountedCost -= coupon.getValue();
                }
                if (discountedCost < 0) discountedCost = 0;
                discountAmount = rentalCost - discountedCost;
                rentalCost = discountedCost;
            } catch (Exception e) {
                // Ignore invalid coupon during calculations
            }
        }

        int totalCost = rentalCost + equipment.getDeposit();

        return PriceCalculationResponse.builder()
                .rentalPrice(rentalCost)
                .deposit(equipment.getDeposit())
                .totalPrice(totalCost)
                .discountAmount(discountAmount)
                .build();
    }

    @Override
    @Transactional
    public BookingDto approveBooking(Long id) {
        Booking booking = getBookingAndVerifyOwnership(id);
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Only pending or active bookings can be approved.");
        }

        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.APPROVED);
        }
        booking = bookingRepository.save(booking);

        notificationService.createInAppNotification(booking.getCustomer().getId(),
                "Booking Approved!",
                String.format("Your rental request for '%s' has been approved by the owner.", booking.getEquipment().getTitle()));

        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto rejectBooking(Long id) {
        Booking booking = getBookingAndVerifyOwnership(id);
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Only pending or active bookings can be rejected.");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking = bookingRepository.save(booking);

        notificationService.createInAppNotification(booking.getCustomer().getId(),
                "Booking Rejected",
                String.format("Your rental request for '%s' was declined. Any processing deposits have been refunded.", booking.getEquipment().getTitle()));

        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto cancelBooking(Long id, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));

        String email = SecurityUtils.getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        boolean isCustomer = booking.getCustomer().getId().equals(currentUser.getId());
        boolean isOwner = booking.getEquipment().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = SecurityUtils.getCurrentUserRoles().contains(RoleName.ROLE_ADMIN.name());

        if (!isCustomer && !isOwner && !isAdmin) {
            throw new ForbiddenException("You do not have permission to cancel this booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cancelled or completed bookings cannot be modified.");
        }

        // Apply cancellation penalty policy (e.g. 50% rental penalty if cancelled within 24 hours of start)
        LocalDateTime now = LocalDateTime.now();
        if (isCustomer && now.isAfter(booking.getStartDate().minusHours(24))) {
            booking.setPenaltyAmount(booking.getRentalPrice() / 2); // 50% penalty
            booking.setSecurityDepositRefunded(true); // Deposit fully refunded
        } else {
            booking.setPenaltyAmount(0);
            booking.setSecurityDepositRefunded(true);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking = bookingRepository.save(booking);

        // Notify other party
        Long notifyUserId = isCustomer ? booking.getEquipment().getOwner().getId() : booking.getCustomer().getId();
        notificationService.createInAppNotification(notifyUserId,
                "Booking Cancelled",
                String.format("The booking for '%s' has been cancelled. Reason: %s.", booking.getEquipment().getTitle(), reason));

        notificationService.sendBookingCancellationEmail(booking.getCustomer().getEmail(),
                booking.getCustomer().getFirstName(), booking.getEquipment().getTitle(), reason);

        log.info("Cancelled booking ID: {} with penalty: {}", id, booking.getPenaltyAmount());
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto requestExtension(Long id, LocalDateTime newEndDate) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));

        String email = SecurityUtils.getCurrentUserEmail();
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Only the renter can request booking extensions.");
        }

        if (booking.getStatus() != BookingStatus.APPROVED && booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Extensions are only allowed for approved or active bookings.");
        }

        if (newEndDate.isBefore(booking.getEndDate()) || newEndDate.isEqual(booking.getEndDate())) {
            throw new BadRequestException("New end date must be after current end date.");
        }

        // Check conflicts for extended duration
        List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.ACTIVE);
        boolean isOverlapping = bookingRepository.existsByEquipmentIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
                booking.getEquipment().getId(), activeStatuses, newEndDate, booking.getEndDate());
        if (isOverlapping) {
            throw new BadRequestException("Extension is unavailable due to an upcoming booking conflict.");
        }

        long extensionDays = ChronoUnit.DAYS.between(booking.getEndDate(), newEndDate);
        if (extensionDays <= 0) extensionDays = 1;
        int extensionPrice = (int) (extensionDays * booking.getDailyRate());

        booking.setIsExtensionRequested(true);
        booking.setExtensionEndDate(newEndDate);
        booking.setExtensionPrice(extensionPrice);
        booking.setExtensionApproved(null);
        booking = bookingRepository.save(booking);

        notificationService.createInAppNotification(booking.getEquipment().getOwner().getId(),
                "Extension Requested",
                String.format("An extension has been requested for '%s' until %s.", booking.getEquipment().getTitle(), newEndDate.toLocalDate()));

        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto approveExtension(Long id) {
        Booking booking = getBookingAndVerifyOwnership(id);
        if (!booking.getIsExtensionRequested()) {
            throw new BadRequestException("No extension request exists for this booking.");
        }

        booking.setEndDate(booking.getExtensionEndDate());
        booking.setTotalPrice(booking.getTotalPrice() + booking.getExtensionPrice());
        booking.setRentalPrice(booking.getRentalPrice() + booking.getExtensionPrice());
        booking.setIsExtensionRequested(false);
        booking.setExtensionEndDate(null);
        booking.setExtensionPrice(null);
        booking.setExtensionApproved(true);
        
        booking = bookingRepository.save(booking);

        // Regenerate Invoice PDF
        String invoiceUrl = invoiceService.generateInvoicePdf(booking);
        booking.setInvoiceUrl(invoiceUrl);
        booking = bookingRepository.save(booking);

        notificationService.createInAppNotification(booking.getCustomer().getId(),
                "Extension Approved!",
                String.format("Your extension request for '%s' has been approved.", booking.getEquipment().getTitle()));

        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return bookingRepository.findByCustomerId(user.getId()).stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getIncomingRequests() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return bookingRepository.findByEquipmentOwnerId(user.getId()).stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        boolean isCustomer = booking.getCustomer().getId().equals(user.getId());
        boolean isOwner = booking.getEquipment().getOwner().getId().equals(user.getId());
        boolean isAdmin = SecurityUtils.getCurrentUserRoles().contains(RoleName.ROLE_ADMIN.name());

        if (!isCustomer && !isOwner && !isAdmin) {
            throw new ForbiddenException("You do not have access to view this booking.");
        }

        return bookingMapper.toDto(booking);
    }

    private Booking getBookingAndVerifyOwnership(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));

        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        boolean isAdmin = SecurityUtils.getCurrentUserRoles().contains(RoleName.ROLE_ADMIN.name());
        if (!booking.getEquipment().getOwner().getId().equals(user.getId()) && !isAdmin) {
            throw new ForbiddenException("You do not own the equipment associated with this booking.");
        }
        return booking;
    }
}
