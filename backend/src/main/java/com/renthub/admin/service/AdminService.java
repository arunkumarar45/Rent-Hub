package com.renthub.admin.service;

import com.renthub.admin.dto.AdminUserDto;
import com.renthub.booking.model.dto.BookingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AdminService {
    Page<AdminUserDto> getAllUsers(Pageable pageable);
    AdminUserDto updateUserRole(Long userId, String roleName);
    AdminUserDto toggleUserBlock(Long userId);
    Page<BookingDto> getAllBookings(Pageable pageable);
    Map<String, Object> getSystemReport();
}
