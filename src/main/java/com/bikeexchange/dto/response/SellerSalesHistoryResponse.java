package com.bikeexchange.dto.response;

import lombok.Data;

@Data
public class SellerSalesHistoryResponse {
    private OrderResponse order;
    private boolean reviewed;
    private ReviewSummaryResponse review;
    private java.util.List<HistoryResponse> history;
}
