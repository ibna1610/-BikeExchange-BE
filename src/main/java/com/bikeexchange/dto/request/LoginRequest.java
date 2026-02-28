package com.bikeexchange.dto.request;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request payload for User Login")
public class LoginRequest {

    @Schema(description = "User's email address used during registration", example = "testbuyer@gmail.com")
    private String email;

    @Schema(description = "User's password", example = "Password123!")
    private String password;
}
