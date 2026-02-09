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
@Table(name = "inspections")
public class Inspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    @ManyToOne
    @JoinColumn(name = "inspector_id", nullable = false)
    private User inspector;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester; // Buyer hoặc Seller yêu cầu

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InspectionStatus status; // PENDING, IN_PROGRESS, APPROVED, REJECTED

    @Column(columnDefinition = "TEXT")
    private String reportDescription;

    @Column(columnDefinition = "TEXT")
    private String frameCondition; // Tình trạng khung

    @Column(columnDefinition = "TEXT")
    private String brakeCondition; // Tình trạng phanh

    @Column(columnDefinition = "TEXT")
    private String drivingCondition; // Tình trạng truyền động

    @Column(columnDefinition = "TEXT")
    private String reportImages; // URL ảnh báo cáo

    @Column(nullable = false)
    private Long inspectionFee;

    @Column(name = "is_paid", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isPaid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = InspectionStatus.PENDING;
        isPaid = false;
    }

    public enum InspectionStatus {
        PENDING, IN_PROGRESS, APPROVED, REJECTED, EXPIRED
    }
}
