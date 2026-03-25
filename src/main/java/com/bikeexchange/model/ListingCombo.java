package com.bikeexchange.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "listing_combos")
public class ListingCombo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "points_cost", nullable = false)
    private Long pointsCost;

    @Column(name = "post_limit", nullable = false)
    private Integer postLimit;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
