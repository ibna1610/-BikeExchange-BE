package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for buyer return request")
public class OrderReturnRequest {

    @Schema(description = "Lý do yêu cầu trả hàng", example = "Xe nhận được có lỗi phanh, không đúng mô tả")
    private String reason;
}
