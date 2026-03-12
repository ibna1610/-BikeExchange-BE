package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for seller delivery update")
public class OrderDeliverRequest {

    @Schema(description = "Tên đơn vị vận chuyển", example = "GHN")
    private String shippingCarrier;

    @Schema(description = "Mã vận đơn", example = "GHN123456789")
    private String trackingCode;

    @Schema(description = "Ghi chú giao hàng", example = "Giao giờ hành chính")
    private String shippingNote;
}
