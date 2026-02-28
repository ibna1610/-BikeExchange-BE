package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class WithdrawRequest {
    private Long amount;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankName;
}
