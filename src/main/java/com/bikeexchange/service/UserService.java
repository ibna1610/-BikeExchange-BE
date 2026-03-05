package com.bikeexchange.service;

import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.PointTransaction;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.repository.PointTransactionRepository;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.UserWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    // Seller upgrade fee in points
    private static final Long SELLER_UPGRADE_FEE = 10000L;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private PointTransactionRepository pointTxRepo;

    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists!");
        }
        return userRepository.save(user);
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User updateUser(Long userId, User updatedUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFullName(updatedUser.getFullName());
        user.setPhone(updatedUser.getPhone());
        user.setAddress(updatedUser.getAddress());
        return userRepository.save(user);
    }

    public User updateUserRating(Long userId, Double newRating) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRating(newRating);
        return userRepository.save(user);
    }

    public User incrementBikesSold(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTotalBikesSold(user.getTotalBikesSold() + 1);
        return userRepository.save(user);
    }

    public User verifyUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsVerified(true);
        return userRepository.save(user);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public User upgradeToSeller(Long userId, String shopName, String shopDescription) {
        // 1. Verify user exists and is BUYER
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getRole() != User.UserRole.BUYER) {
            throw new IllegalArgumentException("Only buyers can upgrade to seller status");
        }
        
        // 2. Check wallet and deduct upgrade fee
        UserWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for userId: " + userId));
        
        if (wallet.getAvailablePoints() < SELLER_UPGRADE_FEE) {
            throw new InsufficientBalanceException(
                    "Insufficient balance to upgrade to seller. Required: " + SELLER_UPGRADE_FEE + 
                    " points, Available: " + wallet.getAvailablePoints() + " points");
        }
        
        // 3. Deduct fee from wallet
        wallet.setAvailablePoints(wallet.getAvailablePoints() - SELLER_UPGRADE_FEE);
        walletRepository.save(wallet);
        
        // 4. Create transaction record for fee
        PointTransaction feeTx = new PointTransaction();
        feeTx.setUser(user);
        feeTx.setAmount(SELLER_UPGRADE_FEE);
        feeTx.setType(PointTransaction.TransactionType.SPEND);
        feeTx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        feeTx.setReferenceId("Seller Upgrade Fee");
        feeTx.setRemarks("Fee charged for upgrading from BUYER to SELLER role");
        pointTxRepo.save(feeTx);
        
        // 5. Update user role and seller info
        user.setRole(User.UserRole.SELLER);
        user.setShopName(shopName);
        user.setShopDescription(shopDescription);
        user.setUpgradedToSellerAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
}
