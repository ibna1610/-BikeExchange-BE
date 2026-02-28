package com.bikeexchange.service;

import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.PointTransaction;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.repository.PointTransactionRepository;
import com.bikeexchange.repository.UserWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletService {

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private PointTransactionRepository pointTxRepo;

    public UserWallet getWallet(Long userId) {
        return walletRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for userId: " + userId));
    }

    public List<PointTransaction> getTransactions(Long userId) {
        return pointTxRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<PointTransaction> getTransactions(Long userId, java.util.List<String> typeParams) {
        if (typeParams == null || typeParams.isEmpty()) {
            return pointTxRepo.findByUserIdOrderByCreatedAtDesc(userId);
        }
        java.util.List<PointTransaction.TransactionType> types = typeParams.stream()
                .map(s -> {
                    try {
                        return PointTransaction.TransactionType.valueOf(s.trim().toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(s -> s != null)
                .toList();
        if (types.isEmpty()) {
            return java.util.List.of();
        }
        return pointTxRepo.findByUserIdAndTypeInOrderByCreatedAtDesc(userId, types);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserWallet depositPoints(Long userId, Long amount, String referenceId) {
        if (amount <= 0)
            throw new IllegalArgumentException("Deposit amount must be > 0");

        // Use Pessimistic locking to avoid race conditions with Escrow / other threads
        UserWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        wallet.setAvailablePoints(wallet.getAvailablePoints() + amount);
        walletRepository.save(wallet);

        PointTransaction tx = new PointTransaction();
        tx.setUser(wallet.getUser());
        tx.setAmount(amount);
        tx.setType(PointTransaction.TransactionType.DEPOSIT);
        tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        tx.setReferenceId(referenceId);
        pointTxRepo.save(tx);

        return wallet;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserWallet requestWithdraw(Long userId, Long amount, String bankName, String bankAccountName,
            String bankAccountNumber) {
        if (amount <= 0)
            throw new IllegalArgumentException("Withdraw amount must be > 0");

        UserWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getAvailablePoints() < amount) {
            throw new InsufficientBalanceException("Not enough available points");
        }

        wallet.setAvailablePoints(wallet.getAvailablePoints() - amount);
        wallet.setFrozenPoints(wallet.getFrozenPoints() + amount); // Freeze until admin approves
        walletRepository.save(wallet);

        PointTransaction tx = new PointTransaction();
        tx.setUser(wallet.getUser());
        tx.setAmount(amount);
        tx.setType(PointTransaction.TransactionType.WITHDRAW);
        tx.setStatus(PointTransaction.TransactionStatus.PENDING); // Admin must approve
        tx.setReferenceId(String.format("Withdrawal: %s | %s | %s", bankName, bankAccountName, bankAccountNumber));
        pointTxRepo.save(tx);

        return wallet;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void approveWithdrawal(Long transactionId) {
        PointTransaction tx = pointTxRepo.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getType() != PointTransaction.TransactionType.WITHDRAW
                || tx.getStatus() != PointTransaction.TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Invalid transaction for approval");
        }

        UserWallet wallet = walletRepository.findByUserIdForUpdate(tx.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        pointTxRepo.save(tx);

        wallet.setFrozenPoints(wallet.getFrozenPoints() - tx.getAmount());
        // Available points were already deducted at request time
        walletRepository.save(wallet);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void rejectWithdrawal(Long transactionId, String reason) {
        PointTransaction tx = pointTxRepo.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getType() != PointTransaction.TransactionType.WITHDRAW
                || tx.getStatus() != PointTransaction.TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Invalid transaction for rejection");
        }

        UserWallet wallet = walletRepository.findByUserIdForUpdate(tx.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        tx.setStatus(PointTransaction.TransactionStatus.FAILED);
        tx.setRemarks(reason);
        pointTxRepo.save(tx);

        // Refund the points
        wallet.setFrozenPoints(wallet.getFrozenPoints() - tx.getAmount());
        wallet.setAvailablePoints(wallet.getAvailablePoints() + tx.getAmount());
        walletRepository.save(wallet);
    }

    public List<PointTransaction> getWithdrawals(java.util.List<PointTransaction.TransactionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return pointTxRepo.findByTypeOrderByCreatedAtDesc(PointTransaction.TransactionType.WITHDRAW);
        }
        return pointTxRepo.findByTypeAndStatusInOrderByCreatedAtDesc(PointTransaction.TransactionType.WITHDRAW,
                statuses);
    }
}
