package com.bikeexchange.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bike_media")
public class BikeMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Bike bike;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public enum MediaType {
        IMAGE, VIDEO
    }
}
