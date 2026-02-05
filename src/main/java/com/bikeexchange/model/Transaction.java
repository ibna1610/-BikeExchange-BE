package com.bikeexchange.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    @Column(nullable = false)
    private Long transactionPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(columnDefinition = "TEXT")
    private String buyerNote;

    @Column(columnDefinition = "TEXT")
    private String sellerNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "buyer_rating")
    private Double buyerRating;

    @Column(name = "seller_rating")
    private Double sellerRating;

    @Column(columnDefinition = "TEXT")
    private String buyerReview;

    @Column(columnDefinition = "TEXT")
    private String sellerReview;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = TransactionStatus.PENDING;
    }

    public enum TransactionStatus {
        PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED, DISPUTED
    }
}
