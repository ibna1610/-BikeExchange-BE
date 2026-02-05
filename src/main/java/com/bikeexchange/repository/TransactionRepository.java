package com.bikeexchange.repository;

import com.bikeexchange.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByBuyerId(Long buyerId, Pageable pageable);

    Page<Transaction> findBySellerId(Long sellerId, Pageable pageable);

    Page<Transaction> findByStatus(Transaction.TransactionStatus status, Pageable pageable);

    List<Transaction> findByBikeId(Long bikeId);

    boolean existsByBikeIdAndStatus(Long bikeId, Transaction.TransactionStatus status);
}
