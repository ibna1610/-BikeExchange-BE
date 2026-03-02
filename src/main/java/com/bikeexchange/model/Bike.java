package com.bikeexchange.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bikes")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Bike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToMany
    @JoinTable(name = "bike_categories", joinColumns = @JoinColumn(name = "bike_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "price_points", nullable = false)
    private Long pricePoints;

    @Column(nullable = false)
    private Integer mileage;

    @Column(name = "`condition`", nullable = false)
    private String condition;

    @Column(nullable = false)
    private String bikeType;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(name = "frame_size")
    private String frameSize;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BikeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_status", length = 30)
    private InspectionStatus inspectionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "bike", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BikeMedia> media = new ArrayList<>();

    @Column(name = "views", columnDefinition = "INT DEFAULT 0")
    private Integer views;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        views = 0;
        if (status == null) {
            status = BikeStatus.DRAFT;
        }
        if (inspectionStatus == null) {
            inspectionStatus = InspectionStatus.NONE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum BikeStatus {
        DRAFT, ACTIVE, VERIFIED, RESERVED, SOLD, CANCELLED
    }

    public enum InspectionStatus {
        NONE, REQUESTED, IN_PROGRESS, APPROVED, REJECTED
    }
}
