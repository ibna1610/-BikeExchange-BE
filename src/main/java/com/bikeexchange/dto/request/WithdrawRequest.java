package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class WithdrawRequest {
    private Long amount;
    private String bankAccountConfig;
}
