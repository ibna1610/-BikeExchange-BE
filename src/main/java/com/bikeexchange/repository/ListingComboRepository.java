package com.bikeexchange.repository;

import com.bikeexchange.model.ListingCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ListingComboRepository extends JpaRepository<ListingCombo, Long> {
    List<ListingCombo> findByIsActiveTrue();
}
