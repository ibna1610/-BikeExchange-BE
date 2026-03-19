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
@Table(name = "disputes_new")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 500)
    private DisputeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type", nullable = false, length = 100)
    private DisputeType disputeType;           // RETURN or GENERAL

    @Column(name = "buyer_contact_address", length = 500)
    private String buyerContactAddress;        // Địa chỉ buyer (từ return-dispute)

    @Column(name = "buyer_contact_phone", length = 120)
    private String buyerContactPhone;          // SĐT buyer

    @Column(name = "buyer_contact_email", length = 255)
    private String buyerContactEmail;          // Email buyer

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = DisputeStatus.OPEN;
    }

    public enum DisputeStatus {
        OPEN, INVESTIGATING, RESOLVED_REFUND, RESOLVED_RELEASE, REJECTED
    }

    public enum DisputeType {
        RETURN,   // Seller không hoàn hàng trả
        GENERAL   // Tranh chấp chung
    }
}
