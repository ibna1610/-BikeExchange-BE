package com.bikeexchange.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeToSellerRequest {
    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Shop description is required")
    private String shopDescription;

    private boolean agreeToTerms;
}
