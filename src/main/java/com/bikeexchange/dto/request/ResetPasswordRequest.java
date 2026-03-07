package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @Schema(description = "The token received via email", example = "e728fbe2-b681-4b93-8acc-a4ae9d432f31")
    private String token;

    @Schema(description = "New password", example = "newPassword123")
    private String newPassword;

    @Schema(description = "Confirmation of the new password", example = "newPassword123")
    private String confirmPassword;
}
