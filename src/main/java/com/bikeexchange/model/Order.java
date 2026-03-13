package com.bikeexchange.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    @Column(name = "amount_points", nullable = false)
    private Long amountPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;          // Seller đánh dấu đã giao

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;           // Seller đã nhận xử lý đơn

    @Column(name = "delivery_proof_image_url", length = 500)
    private String deliveryProofImageUrl;       // Ảnh minh chứng đã giao

    @Column(name = "delivery_proof_image_url_2", length = 500)
    private String deliveryProofImageUrl2;      // Ảnh minh chứng đã giao (ảnh 2)

    @Column(name = "shipping_carrier", length = 120)
    private String shippingCarrier;             // Đơn vị vận chuyển

    @Column(name = "tracking_code", length = 120)
    private String trackingCode;                // Mã vận đơn

    @Column(name = "shipping_note", length = 500)
    private String shippingNote;                // Ghi chú giao hàng

    @Column(name = "return_reason", length = 1000)
    private String returnReason;                // Lý do buyer yêu cầu trả hàng

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)
            status = OrderStatus.PENDING_PAYMENT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        PENDING_PAYMENT,
        ESCROWED,
        ACCEPTED,            // Seller đã accept đơn, chuẩn bị gửi hàng
        DELIVERED,           // Seller đánh dấu đã giao hàng; bắt đầu đếm 14 ngày
        RETURN_REQUESTED,    // Buyer yêu cầu hoàn hàng (trong 14 ngày kể từ deliveredAt)
        COMPLETED,           // Điểm giải phóng về seller
        REFUNDED,            // Điểm hoàn về buyer sau khi seller xác nhận nhận lại hàng
        DISPUTED,
        CANCELLED
    }
}
