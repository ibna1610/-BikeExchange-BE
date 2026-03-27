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

        java.util.List<PointTransaction> findByTypeAndStatusInOrderByCreatedAtDesc(
                        PointTransaction.TransactionType type,
                        java.util.List<PointTransaction.TransactionStatus> statuses);

        Optional<PointTransaction> findByReferenceId(String referenceId);

        java.util.List<PointTransaction> findAllByOrderByCreatedAtDesc();

        java.util.List<PointTransaction> findByStatusInOrderByCreatedAtDesc(
                        java.util.List<PointTransaction.TransactionStatus> statuses);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(t.amount) FROM PointTransaction t WHERE t.type = :type AND t.status = :status")
        Long sumAmountByTypeAndStatus(@org.springframework.data.repository.query.Param("type") PointTransaction.TransactionType type,
                        @org.springframework.data.repository.query.Param("status") PointTransaction.TransactionStatus status);
        @org.springframework.data.jpa.repository.Query("SELECT SUM(t.amount) FROM PointTransaction t WHERE t.type = :type AND t.status IN :statuses")
        Long sumAmountByTypeAndStatusIn(@org.springframework.data.repository.query.Param("type") PointTransaction.TransactionType type,
                        @org.springframework.data.repository.query.Param("statuses") java.util.List<PointTransaction.TransactionStatus> statuses);
}
