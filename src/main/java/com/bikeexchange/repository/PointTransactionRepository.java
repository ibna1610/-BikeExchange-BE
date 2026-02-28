package com.bikeexchange.repository;

import com.bikeexchange.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
