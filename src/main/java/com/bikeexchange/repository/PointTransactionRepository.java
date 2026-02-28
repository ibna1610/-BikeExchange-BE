package com.bikeexchange.repository;

import com.bikeexchange.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    java.util.List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    java.util.List<PointTransaction> findByUserIdAndTypeInOrderByCreatedAtDesc(Long userId, java.util.List<PointTransaction.TransactionType> types);
    Optional<PointTransaction> findByReferenceId(String referenceId);
}
