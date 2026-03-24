package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class OrderRuleConfigRequest {
    private Double commissionRate;
    private Long sellerUpgradeFee;
    private Integer returnWindowDays;
}
