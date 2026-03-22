package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class AdminOrderRuleUpdateRequest {
    private Double commissionRate;
    private Long sellerUpgradeFee;
    private Integer returnWindowDays;
}