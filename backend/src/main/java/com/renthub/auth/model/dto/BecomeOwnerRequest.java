package com.renthub.auth.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BecomeOwnerRequest {
    @AssertTrue(message = "You must agree to the Owner Marketplace Terms and Conditions")
    private Boolean termsAccepted;

    @NotBlank(message = "Payout account details or bank details are required for owner payouts")
    private String payoutDetails;
}
