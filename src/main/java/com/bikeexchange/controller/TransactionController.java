package com.bikeexchange.controller;

import com.bikeexchange.model.Transaction;
import com.bikeexchange.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @RequestParam Long bikeId,
            @RequestParam Long buyerId) {
        try {
            Transaction transaction = transactionService.createTransaction(bikeId, buyerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long transactionId) {
        return transactionService.getTransactionById(transactionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<Page<Transaction>> getBuyerTransactions(
            @PathVariable Long buyerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getBuyerTransactions(buyerId, pageable);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<Transaction>> getSellerTransactions(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getSellerTransactions(sellerId, pageable);
        return ResponseEntity.ok(transactions);
    }

    @PutMapping("/{transactionId}/accept")
    public ResponseEntity<Transaction> acceptTransaction(@PathVariable Long transactionId) {
        try {
            Transaction transaction = transactionService.acceptTransaction(transactionId);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{transactionId}/start")
    public ResponseEntity<Transaction> startTransaction(@PathVariable Long transactionId) {
        try {
            Transaction transaction = transactionService.startTransaction(transactionId);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{transactionId}/complete")
    public ResponseEntity<Transaction> completeTransaction(@PathVariable Long transactionId) {
        try {
            Transaction transaction = transactionService.completeTransaction(transactionId);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{transactionId}/cancel")
    public ResponseEntity<Transaction> cancelTransaction(@PathVariable Long transactionId) {
        try {
            Transaction transaction = transactionService.cancelTransaction(transactionId);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{transactionId}/rate")
    public ResponseEntity<Transaction> rateTransaction(
            @PathVariable Long transactionId,
            @RequestParam Double buyerRating,
            @RequestParam String buyerReview,
            @RequestParam Double sellerRating,
            @RequestParam String sellerReview) {
        try {
            Transaction transaction = transactionService.rateTransaction(
                    transactionId, buyerRating, buyerReview, sellerRating, sellerReview);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Transaction>> getTransactionsByStatus(
            @PathVariable Transaction.TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getTransactionsByStatus(status, pageable);
        return ResponseEntity.ok(transactions);
    }
}
