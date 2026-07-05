package com.renthub.booking.service;

import com.renthub.auth.model.entity.User;
import com.renthub.auth.repository.UserRepository;
import com.renthub.booking.mapper.BookingMapper;
import com.renthub.booking.model.dto.BookingDto;
import com.renthub.booking.model.dto.BookingRequest;
import com.renthub.booking.model.entity.Booking;
import com.renthub.booking.model.entity.BookingStatus;
import com.renthub.booking.repository.BookingRepository;
import com.renthub.common.exception.BadRequestException;
import com.renthub.coupon.service.CouponService;
import com.renthub.equipment.model.entity.Equipment;
import com.renthub.equipment.repository.EquipmentAvailabilityRepository;
import com.renthub.equipment.repository.EquipmentRepository;
import com.renthub.equipment.repository.SpecialPricingRepository;
import com.renthub.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private EquipmentAvailabilityRepository availabilityRepository;
    @Mock
    private SpecialPricingRepository specialPricingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CouponService couponService;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User customer;
    private User owner;
    private Equipment equipment;
    private Booking booking;
    private BookingDto bookingDto;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(2L).email("renter@example.com").firstName("Jane").lastName("Smith").build();
        owner = User.builder().id(1L).email("owner@example.com").firstName("John").lastName("Doe").build();
        
        equipment = Equipment.builder()
                .id(10L)
                .owner(owner)
                .title("Camera")
                .dailyRate(1000) // $10
                .deposit(500)   // $5
                .isActive(true)
                .build();

        booking = Booking.builder()
                .id(1L)
                .equipment(equipment)
                .customer(customer)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .dailyRate(1000)
                .deposit(500)
                .rentalPrice(2000)
                .totalPrice(2500)
                .status(BookingStatus.PENDING)
                .build();

        bookingDto = BookingDto.builder()
                .id(1L)
                .equipmentId(10L)
                .customerId(2L)
                .status(BookingStatus.PENDING)
                .totalPrice(2500)
                .build();
    }

    @Test
    void createBooking_Conflict_ThrowsException() {
        setupSecurityContext("renter@example.com");

        when(userRepository.findByEmail("renter@example.com")).thenReturn(Optional.of(customer));
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipment));
        
        // Mock overlapping check returning true
        when(bookingRepository.existsByEquipmentIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
                anyLong(), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        BookingRequest request = BookingRequest.builder()
                .equipmentId(10L)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .build();

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(request));
    }

    private void setupSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
