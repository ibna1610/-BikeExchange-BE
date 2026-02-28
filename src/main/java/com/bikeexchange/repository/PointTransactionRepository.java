package com.bikeexchange.repository;

import com.bikeexchange.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    java.util.List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    java.util.List<PointTransaction> findByUserIdAndTypeInOrderByCreatedAtDesc(Long userId,
            java.util.List<PointTransaction.TransactionType> types);

    java.util.List<PointTransaction> findByTypeOrderByCreatedAtDesc(PointTransaction.TransactionType type);

    java.util.List<PointTransaction> findByTypeAndStatusOrderByCreatedAtDesc(PointTransaction.TransactionType type,
            PointTransaction.TransactionStatus status);

    java.util.List<PointTransaction> findByTypeAndStatusInOrderByCreatedAtDesc(PointTransaction.TransactionType type,
            java.util.List<PointTransaction.TransactionStatus> statuses);

    Optional<PointTransaction> findByReferenceId(String referenceId);
}
