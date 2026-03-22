package com.bikeexchange.repository;

import com.bikeexchange.model.OrderRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRuleConfigRepository extends JpaRepository<OrderRuleConfig, Long> {
}