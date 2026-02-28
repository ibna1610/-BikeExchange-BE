package com.bikeexchange.dto.request;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request payload for User Registration")
public class RegisterRequest {

    @Schema(description = "User's email address", example = "testbuyer@gmail.com")
    private String email;

    @Schema(description = "User's password", example = "Password123!")
    private String password;

    @Schema(description = "User's full name", example = "Nguyen Van A")
    private String fullName;

    @Schema(description = "User's phone number", example = "0987654321")
    private String phone;

    @Schema(description = "User's primary address", example = "123 Le Loi, D1, HCMC")
    private String address;
}
