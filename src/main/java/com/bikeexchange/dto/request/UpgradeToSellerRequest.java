package com.bikeexchange.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user upgrade request from Buyer to Seller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeToSellerRequest {
    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Shop description is required")
    private String shopDescription;

    @NotNull(message = "Agreement acceptance is required")
    private Boolean agreeToTerms;
}
