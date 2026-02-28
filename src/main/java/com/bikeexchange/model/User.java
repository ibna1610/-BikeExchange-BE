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
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "rating", columnDefinition = "DECIMAL(3,2) DEFAULT 5.0")
    private Double rating;

    @Column(name = "total_bikes_sold", columnDefinition = "INT DEFAULT 0")
    private Integer totalBikesSold;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_verified", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isVerified;

    @Column(name = "status", columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private String status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        totalBikesSold = 0;
        rating = 5.0;
        isVerified = false;
        if (status == null)
            status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UserRole {
        GUEST, BUYER, SELLER, INSPECTOR, ADMIN
    }
}
