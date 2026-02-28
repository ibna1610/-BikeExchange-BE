package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class DisputeCreateRequest {
    private Long orderId;
    private String reason;
}
