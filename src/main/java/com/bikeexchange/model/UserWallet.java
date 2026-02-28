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
@Table(name = "user_wallets")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class UserWallet {
    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "available_points", nullable = false)
    private Long availablePoints = 0L;

    @Column(name = "frozen_points", nullable = false)
    private Long frozenPoints = 0L;

    @Version
    private Long version; // For Optimistic Locking if needed as secondary protection

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
