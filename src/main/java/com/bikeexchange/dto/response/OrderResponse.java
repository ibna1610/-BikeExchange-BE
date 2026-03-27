package com.bikeexchange.dto.response;

import com.bikeexchange.model.Order;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    private LocalDateTime acceptedAt;
    private LocalDateTime deliveredAt;
    private String deliveryProofImageUrl;
    private String deliveryProofImageUrl2;
    private String shippingCarrier;
    private String trackingCode;
    private String shippingNote;
    private String returnReason;
    private Long daysUntilAutoRelease;
    // Thêm các trường chi tiết cho FE
    private Long remainingDays;
    private Long remainingHours;
    private Long remainingMinutes;
    private Long remainingSeconds;
    private LocalDateTime autoReleaseDeadline;

    public static OrderResponse fromEntity(Order order) {
        return fromEntity(order, 0L);
    }

    public static OrderResponse fromEntity(Order order, long returnWindowTotalMinutes) {
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
        res.setAcceptedAt(order.getAcceptedAt());
        res.setDeliveredAt(order.getDeliveredAt());
        res.setDeliveryProofImageUrl(order.getDeliveryProofImageUrl());
        res.setDeliveryProofImageUrl2(order.getDeliveryProofImageUrl2());
        res.setShippingCarrier(order.getShippingCarrier());
        res.setTrackingCode(order.getTrackingCode());
        res.setShippingNote(order.getShippingNote());
        res.setReturnReason(order.getReturnReason());
        if (returnWindowTotalMinutes > 0
                && order.getDeliveredAt() != null
                && order.getStatus() == Order.OrderStatus.DELIVERED) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = order.getDeliveredAt().plusMinutes(returnWindowTotalMinutes);
            long totalRemainingSeconds = Math.max(0L, ChronoUnit.SECONDS.between(now, deadline));
            long remainingDays = totalRemainingSeconds / (24 * 3600);
            long remainingHours = (totalRemainingSeconds % (24 * 3600)) / 3600;
            long remainingMinutes = (totalRemainingSeconds % 3600) / 60;
            long remainingSeconds = totalRemainingSeconds % 60;
            res.setDaysUntilAutoRelease(remainingDays); // giữ trường cũ cho tương thích
            res.setRemainingDays(remainingDays);
            res.setRemainingHours(remainingHours);
            res.setRemainingMinutes(remainingMinutes);
            res.setRemainingSeconds(remainingSeconds);
            res.setAutoReleaseDeadline(deadline);
        }
        return res;
    }
}
