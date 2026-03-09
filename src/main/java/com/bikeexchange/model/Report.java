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
@Table(name = "reports")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id")
    private Bike bike; // optional

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // optional - reported user

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 30)
    private ReportType reportType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReportStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public enum ReportType {
        SPAM, FRAUD, INAPPROPRIATE, OFFENSIVE_LANGUAGE, FAKE_ITEM, OTHER
    }

    public enum ReportStatus {
        PENDING, REVIEWING, RESOLVED, REJECTED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReportStatus.PENDING;
        }
    }
}
