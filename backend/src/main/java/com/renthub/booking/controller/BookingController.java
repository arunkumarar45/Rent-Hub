package com.renthub.booking.controller;

import com.renthub.booking.model.dto.BookingDto;
import com.renthub.booking.model.dto.BookingRequest;
import com.renthub.booking.service.BookingService;
import com.renthub.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Booking & Rental Lifecycle", description = "Endpoints for scheduling bookings, approvals, rejection, extensions, and cancellation policies.")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request booking", description = "Initiates a pending rental booking request. Performs availability conflict checks and price calculations.")
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingDto created = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Booking requested successfully."));
    }

    @PostMapping("/calculate-price")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Calculate booking price preview", description = "Calculates rental subtotal, dynamic rates, and coupon discount amounts before confirming a booking.")
    public ResponseEntity<ApiResponse<com.renthub.booking.model.dto.PriceCalculationResponse>> calculatePrice(
            @Valid @RequestBody BookingRequest request) {
        com.renthub.booking.model.dto.PriceCalculationResponse response = bookingService.calculatePrice(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Price calculation completed successfully."));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking details", description = "Retrieves details of a booking. Access is restricted to the renter, equipment owner, or administrator.")
    public ResponseEntity<ApiResponse<BookingDto>> getBookingById(@PathVariable Long id) {
        BookingDto booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking details retrieved successfully."));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Approve rental request", description = "Approves a pending rental request. Requires ownership of the equipment.")
    public ResponseEntity<ApiResponse<BookingDto>> approveBooking(@PathVariable Long id) {
        BookingDto approved = bookingService.approveBooking(id);
        return ResponseEntity.ok(ApiResponse.success(approved, "Booking request approved."));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Reject rental request", description = "Declines a pending rental request. Deposit will be marked as refunded.")
    public ResponseEntity<ApiResponse<BookingDto>> rejectBooking(@PathVariable Long id) {
        BookingDto rejected = bookingService.rejectBooking(id);
        return ResponseEntity.ok(ApiResponse.success(rejected, "Booking request declined."));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel booking", description = "Cancels an active/approved booking. Penalty calculations apply if cancelled within 24h.")
    public ResponseEntity<ApiResponse<BookingDto>> cancelBooking(
            @PathVariable Long id,
            @RequestParam String reason) {
        BookingDto cancelled = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(ApiResponse.success(cancelled, "Booking cancelled successfully."));
    }

    @PostMapping("/{id}/extend")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request booking extension", description = "Submits an extension request to prolong the rental period. Verifies calendar overlaps.")
    public ResponseEntity<ApiResponse<BookingDto>> requestExtension(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newEndDate) {
        BookingDto extended = bookingService.requestExtension(id, newEndDate);
        return ResponseEntity.ok(ApiResponse.success(extended, "Extension requested successfully."));
    }

    @PatchMapping("/{id}/extend/approve")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Approve extension request", description = "Confirms and applies a renter's extension request. Regenerates the invoice PDF.")
    public ResponseEntity<ApiResponse<BookingDto>> approveExtension(@PathVariable Long id) {
        BookingDto approved = bookingService.approveExtension(id);
        return ResponseEntity.ok(ApiResponse.success(approved, "Extension request approved."));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my bookings", description = "Lists all bookings placed by the authenticated customer.")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getMyBookings() {
        List<BookingDto> myBookings = bookingService.getMyBookings();
        return ResponseEntity.ok(ApiResponse.success(myBookings, "My bookings retrieved successfully."));
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get incoming rental requests", description = "Lists all booking requests incoming for the owner's listings.")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getIncomingRequests() {
        List<BookingDto> incoming = bookingService.getIncomingRequests();
        return ResponseEntity.ok(ApiResponse.success(incoming, "Incoming requests retrieved successfully."));
    }
}
