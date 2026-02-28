package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for creating a new order")
public class OrderCreateRequest {

    @Schema(description = "ID of the bike to buy", example = "10")
    private Long bikeId;

    @Schema(description = "Unique key to prevent duplicate orders (Client-generated UUID recommended)", example = "333e4444-e89b-12d3-a456-426614174000")
    private String idempotencyKey;
}
