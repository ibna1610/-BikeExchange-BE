package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for creating a new order")
public class OrderCreateRequest {

    @Schema(description = "ID of the bike to buy", example = "10")
    private Long bikeId;

    @Schema(hidden = true)
    private String idempotencyKey;
}
