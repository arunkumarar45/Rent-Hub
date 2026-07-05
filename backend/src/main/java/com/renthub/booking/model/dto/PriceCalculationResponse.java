package com.renthub.booking.model.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationResponse {
    private int rentalPrice;
    private int deposit;
    private int totalPrice;
    private int discountAmount;
}
