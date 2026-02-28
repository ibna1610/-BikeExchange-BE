package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class DepositRequest {
    private Long amount;
    private String referenceId; // e.g., VNPay or Momo transaction ID
}
