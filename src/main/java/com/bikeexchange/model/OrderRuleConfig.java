package com.bikeexchange.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "order_rule_configs")
public class OrderRuleConfig {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "commission_rate", nullable = false)
    private Double commissionRate;

    @Column(name = "seller_upgrade_fee", nullable = false)
    private Long sellerUpgradeFee;

    @Column(name = "return_window_days", nullable = false)
    private Integer returnWindowDays = 1;
    
    @Column(name = "return_window_hours", nullable = false)
    private Integer returnWindowHours = 0;
    
    @Column(name = "return_window_minutes", nullable = false)
    private Integer returnWindowMinutes = 0;

    @Column(name = "bike_post_fee", nullable = false)
    private Long bikePostFee;

    @Column(name = "inspection_fee", nullable = false)
    private Long inspectionFee;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
