package com.renthub.booking.service;

import com.renthub.booking.model.dto.BookingDto;
import com.renthub.booking.model.dto.BookingRequest;

import com.renthub.booking.model.dto.PriceCalculationResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {
    BookingDto createBooking(BookingRequest request);
    PriceCalculationResponse calculatePrice(BookingRequest request);
    BookingDto approveBooking(Long id);
    BookingDto rejectBooking(Long id);
    BookingDto cancelBooking(Long id, String reason);
    BookingDto requestExtension(Long id, LocalDateTime newEndDate);
    BookingDto approveExtension(Long id);
    
    List<BookingDto> getMyBookings();
    List<BookingDto> getIncomingRequests();
    BookingDto getBookingById(Long id);
}
