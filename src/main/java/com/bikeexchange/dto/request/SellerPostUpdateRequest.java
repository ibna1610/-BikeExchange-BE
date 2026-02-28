package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SellerPostUpdateRequest {

    @Schema(description = "Post caption", example = "Cập nhật mô tả, còn mới 90%")
    private String caption;
}
