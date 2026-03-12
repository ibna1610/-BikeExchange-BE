package com.bikeexchange.dto.response;

import lombok.Data;

@Data
public class OrderHistoryDetailResponse {
    private String viewerRole;
    private OrderResponse order;
    private boolean canReview;
    private boolean reviewed;
    private ReviewSummaryResponse review;
    private java.util.List<HistoryResponse> history;
}
