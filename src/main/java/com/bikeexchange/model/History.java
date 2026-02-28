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
@Table(name = "histories")
public class History {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
