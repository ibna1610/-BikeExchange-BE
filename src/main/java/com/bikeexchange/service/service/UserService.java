package com.bikeexchange.service.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import com.bikeexchange.dto.request.UserUpdateRequest;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private PointTransactionRepository pointTxRepo;

    @Autowired
    private OrderRuleConfigService orderRuleConfigService;

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

    public User updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        
        // If password is provided in the request, encrypt and update it
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
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
        String normalizedShopName = shopName == null ? "" : shopName.trim();
        String normalizedShopDescription = shopDescription == null ? "" : shopDescription.trim();

        if (normalizedShopName.length() < 3 || normalizedShopName.length() > 100) {
            throw new IllegalArgumentException("Shop name must be between 3 and 100 characters");
        }
        if (normalizedShopDescription.length() < 20 || normalizedShopDescription.length() > 500) {
            throw new IllegalArgumentException("Shop description must be between 20 and 500 characters");
        }

        long sellerUpgradeFee = orderRuleConfigService.getSellerUpgradeFee();

        // 1. Verify user exists and is BUYER
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getRole() != User.UserRole.BUYER) {
            throw new IllegalArgumentException("Only buyers can upgrade to seller status");
        }
        
        // 2. Check wallet and deduct upgrade fee
        UserWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for userId: " + userId));
        
        if (wallet.getAvailablePoints() < sellerUpgradeFee) {
            throw new InsufficientBalanceException(
                "Insufficient balance to upgrade to seller. Required: " + sellerUpgradeFee +
                    " points, Available: " + wallet.getAvailablePoints() + " points");
        }
        
        // 3. Deduct fee from wallet
        wallet.setAvailablePoints(wallet.getAvailablePoints() - sellerUpgradeFee);
        walletRepository.save(wallet);
        
        // 4. Create transaction record for fee
        PointTransaction feeTx = new PointTransaction();
        feeTx.setUser(user);
        feeTx.setAmount(sellerUpgradeFee);
        feeTx.setType(PointTransaction.TransactionType.SPEND);
        feeTx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        feeTx.setReferenceId("Seller Upgrade Fee");
        feeTx.setRemarks("Fee charged for upgrading from BUYER to SELLER role");
        pointTxRepo.save(feeTx);
        
        // 5. Update user role and seller info
        user.setRole(User.UserRole.SELLER);
        user.setShopName(normalizedShopName);
        user.setShopDescription(normalizedShopDescription);
        user.setRating(0.0); // Reset rating to 0.0 for new seller
        user.setUpgradedToSellerAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public long getSellerUpgradeFee() {
        return orderRuleConfigService.getSellerUpgradeFee();
    }
}
