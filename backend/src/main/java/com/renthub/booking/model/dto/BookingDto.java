package com.renthub.booking.model.dto;

import com.renthub.booking.model.entity.BookingStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {
    private Long id;
    private Long equipmentId;
    private String equipmentTitle;
    private String equipmentImageUrl;
    private Long ownerId;
    private Long customerId;
    private String customerName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer dailyRate; // cents
    private Integer deposit; // cents
    private Integer rentalPrice; // cents
    private Integer totalPrice; // cents
    private BookingStatus status;
    private String qrCode;
    private String cancellationReason;
    private Boolean isExtensionRequested;
    private LocalDateTime extensionEndDate;
    private Integer extensionPrice;
    private Boolean extensionApproved;
    private Boolean securityDepositRefunded;
    private Integer penaltyAmount;
    private String invoiceUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
