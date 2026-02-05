package com.bikeexchange.service;

import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Transaction;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BikeService bikeService;

    @Autowired
    private UserService userService;

    public Transaction createTransaction(Long bikeId, Long buyerId) {
        Bike bike = bikeService.getBikeById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));

        if (!bike.getStatus().equals(Bike.BikeStatus.AVAILABLE)) {
            throw new IllegalArgumentException("Bike is not available for purchase");
        }

        User buyer = userService.getUserById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        Transaction transaction = new Transaction();
        transaction.setBike(bike);
        transaction.setBuyer(buyer);
        transaction.setSeller(bike.getSeller());
        transaction.setTransactionPrice(bike.getPrice());
        transaction.setStatus(Transaction.TransactionStatus.PENDING);

        bikeService.reserveBike(bikeId);

        return transactionRepository.save(transaction);
    }

    public Optional<Transaction> getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId);
    }

    public Page<Transaction> getBuyerTransactions(Long buyerId, Pageable pageable) {
        return transactionRepository.findByBuyerId(buyerId, pageable);
    }

    public Page<Transaction> getSellerTransactions(Long sellerId, Pageable pageable) {
        return transactionRepository.findBySellerId(sellerId, pageable);
    }

    public Transaction acceptTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getStatus().equals(Transaction.TransactionStatus.PENDING)) {
            throw new IllegalArgumentException("Transaction cannot be accepted in current status");
        }

        transaction.setStatus(Transaction.TransactionStatus.ACCEPTED);
        return transactionRepository.save(transaction);
    }

    public Transaction startTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus(Transaction.TransactionStatus.IN_PROGRESS);
        return transactionRepository.save(transaction);
    }

    public Transaction completeTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());

        bikeService.markBikeAsSold(transaction.getBike().getId());
        userService.incrementBikesSold(transaction.getSeller().getId());

        return transactionRepository.save(transaction);
    }

    public Transaction cancelTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getStatus().equals(Transaction.TransactionStatus.COMPLETED)) {
            throw new IllegalArgumentException("Cannot cancel a completed transaction");
        }

        transaction.setStatus(Transaction.TransactionStatus.CANCELLED);

        if (transaction.getBike().getStatus().equals(Bike.BikeStatus.RESERVED)) {
            Bike bike = transaction.getBike();
            bike.setStatus(Bike.BikeStatus.AVAILABLE);
            bikeService.updateBike(bike.getId(), bike);
        }

        return transactionRepository.save(transaction);
    }

    public Transaction rateTransaction(Long transactionId, Double buyerRating, String buyerReview,
                                      Double sellerRating, String sellerReview) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setBuyerRating(buyerRating);
        transaction.setBuyerReview(buyerReview);
        transaction.setSellerRating(sellerRating);
        transaction.setSellerReview(sellerReview);

        return transactionRepository.save(transaction);
    }

    public Page<Transaction> getTransactionsByStatus(Transaction.TransactionStatus status, Pageable pageable) {
        return transactionRepository.findByStatus(status, pageable);
    }
}
