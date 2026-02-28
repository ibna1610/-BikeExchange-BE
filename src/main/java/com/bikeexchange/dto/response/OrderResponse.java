package com.bikeexchange.dto.response;

import com.bikeexchange.model.Order;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderResponse {
    private Long id;
    private Long buyerId;
    private String buyerName;
    private Long bikeId;
    private String bikeTitle;
    private Long sellerId;
    private String sellerName;
    private Long amountPoints;
    private Order.OrderStatus status;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse fromEntity(Order order) {
        OrderResponse res = new OrderResponse();
        res.setId(order.getId());
        if (order.getBuyer() != null) {
            res.setBuyerId(order.getBuyer().getId());
            res.setBuyerName(order.getBuyer().getFullName());
        }
        if (order.getBike() != null) {
            res.setBikeId(order.getBike().getId());
            res.setBikeTitle(order.getBike().getTitle());
            if (order.getBike().getSeller() != null) {
                res.setSellerId(order.getBike().getSeller().getId());
                res.setSellerName(order.getBike().getSeller().getFullName());
            }
        }
        res.setAmountPoints(order.getAmountPoints());
        res.setStatus(order.getStatus());
        res.setIdempotencyKey(order.getIdempotencyKey());
        res.setCreatedAt(order.getCreatedAt());
        res.setUpdatedAt(order.getUpdatedAt());
        return res;
    }
}
