package com.bikeexchange.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtAuthResponse {
    private String accessToken;
    private String tokenType;
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String role;

    public JwtAuthResponse(String accessToken, Long id, String email, String fullName, String phone, String role) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
    }
}
