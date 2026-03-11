package com.bikeexchange.repository;

import com.bikeexchange.model.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComponentRepository extends JpaRepository<Component, Long> {
    Optional<Component> findByName(String name);
}
