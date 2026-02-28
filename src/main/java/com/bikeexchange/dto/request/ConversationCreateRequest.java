package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to create a new chat conversation")
public class ConversationCreateRequest {
    @Schema(description = "ID of the bike listing", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bikeId;
}
