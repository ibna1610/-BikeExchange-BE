package com.bikeexchange.dto.response;

import com.bikeexchange.model.Order;
import com.bikeexchange.model.Review;
import lombok.Data;

import java.util.List;

@Data
public class BuyerPurchaseHistoryResponse {
    private OrderResponse order;
    private boolean canReview;
    private boolean reviewed;
    private ReviewSummaryResponse review;
    private List<HistoryResponse> history;

    public static BuyerPurchaseHistoryResponse from(Order order, Review review, List<HistoryResponse> history) {
        BuyerPurchaseHistoryResponse response = new BuyerPurchaseHistoryResponse();
        response.setOrder(OrderResponse.fromEntity(order));
        response.setReviewed(review != null);
        response.setCanReview(order.getStatus() == Order.OrderStatus.COMPLETED && review == null);
        response.setReview(review != null ? ReviewSummaryResponse.fromEntity(review) : null);
        response.setHistory(history);
        return response;
    }
}
