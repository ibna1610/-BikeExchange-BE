package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for buyer to report seller refusing return")
public class ReturnDisputeRequest {

    @Schema(description = "Lý do mở tranh chấp", example = "Người bán từ chối hoàn hàng trả, không phản hồi")
    private String reason;

    @Schema(description = "Địa chỉ của người mua", example = "123 Nguyễn Huệ, Quận 1, TP.HCM")
    private String buyerAddress;

    @Schema(description = "Số điện thoại của người mua", example = "0901000001")
    private String buyerPhone;

    @Schema(description = "Email của người mua", example = "buyer@example.com")
    private String buyerEmail;
}
