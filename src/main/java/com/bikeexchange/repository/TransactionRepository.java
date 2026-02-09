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
    List<Transaction> findByBuyerId(Long buyerId);

    Page<Transaction> findBySellerId(Long sellerId, Pageable pageable);
    List<Transaction> findBySellerId(Long sellerId);

    Page<Transaction> findByStatus(Transaction.TransactionStatus status, Pageable pageable);

    List<Transaction> findByBikeId(Long bikeId);
    List<Transaction> findByBikeIdAndStatus(Long bikeId, Transaction.TransactionStatus status);

    boolean existsByBikeIdAndStatus(Long bikeId, Transaction.TransactionStatus status);

    List<Transaction> findBySellerIdAndStatus(Long sellerId, Transaction.TransactionStatus status);
}
