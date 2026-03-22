package com.bikeexchange.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeToSellerRequest {
    @NotBlank(message = "Shop name is required")
    @Size(min = 3, max = 100, message = "Shop name must be between 3 and 100 characters")
    private String shopName;

    @NotBlank(message = "Shop description is required")
    @Size(min = 20, max = 500, message = "Shop description must be between 20 and 500 characters")
    private String shopDescription;

    private boolean agreeToTerms;

    @NotBlank(message = "Confirmation phrase is required")
    private String confirmPhrase;

    private boolean acceptBusinessResponsibility;
}
