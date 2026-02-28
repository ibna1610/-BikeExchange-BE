package com.bikeexchange.repository;

import com.bikeexchange.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PointTransaction> findByReferenceId(String referenceId);
}
