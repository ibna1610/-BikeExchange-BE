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
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter; // Người báo cáo

    @ManyToOne
    @JoinColumn(name = "bike_id")
    private Bike bike; // Xe bị báo cáo (nếu có)

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User reportedUser; // Người dùng bị báo cáo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType; // SPAM, FRAUD, INAPPROPRIATE, OTHER

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status; // PENDING, REVIEWING, RESOLVED, REJECTED

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = ReportStatus.PENDING;
    }

    public enum ReportType {
        SPAM, FRAUD, INAPPROPRIATE, OFFENSIVE_LANGUAGE, FAKE_ITEM, OTHER
    }

    public enum ReportStatus {
        PENDING, REVIEWING, RESOLVED, REJECTED
    }
}
