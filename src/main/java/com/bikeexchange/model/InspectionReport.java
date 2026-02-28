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
@Table(name = "inspection_reports")
public class InspectionReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private InspectionRequest request;

    @Column(name = "frame_condition")
    private String frameCondition;

    @Column(name = "groupset_condition")
    private String groupsetCondition;

    @Column(name = "wheel_condition")
    private String wheelCondition;

    @Column(name = "overall_score")
    private Integer overallScore; // out of 100 or 10

    @Column(name = "admin_decision")
    @Enumerated(EnumType.STRING)
    private InspectionRequest.RequestStatus adminDecision; // APPROVED / REJECTED

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
