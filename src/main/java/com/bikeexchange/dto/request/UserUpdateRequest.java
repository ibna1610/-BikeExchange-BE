package com.bikeexchange.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    // Password can be optional on update. If empty, it means we don't change it.
    private String password;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Address is required")
    private String address;
}
