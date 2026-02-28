package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SellerPostCreateRequest {

    @Schema(description = "Bike id to post", example = "10")
    private Long bikeId;

    @Schema(description = "Post caption", example = "Xe mới 95%, bao test")
    private String caption;

    @Schema(description = "Post type: VERIFIED or STANDARD", example = "STANDARD")
    private String listingType;
}
