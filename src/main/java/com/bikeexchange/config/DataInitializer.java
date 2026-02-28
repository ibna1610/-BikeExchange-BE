package com.bikeexchange.config;

import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.UserWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedUser("admin@bikeexchange.com", "System Admin", User.UserRole.ADMIN);
        seedUser("seller@bikeexchange.com", "Sample Seller", User.UserRole.SELLER);
        seedUser("inspector@bikeexchange.com", "Sample Inspector", User.UserRole.INSPECTOR);
        seedUser("buyer@bikeexchange.com", "Sample Buyer", User.UserRole.BUYER);
    }

    private void seedUser(String email, String fullName, User.UserRole role) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("password123"));
            user.setFullName(fullName);
            user.setPhone("0987654321");
            user.setAddress("System Default Address");
            user.setRole(role);
            user.setStatus("ACTIVE");
            user.setIsVerified(true);

            User savedUser = userRepository.save(user);

            // Create Wallet
            if (!walletRepository.existsById(savedUser.getId())) {
                UserWallet wallet = new UserWallet();
                wallet.setUser(savedUser);
                wallet.setAvailablePoints(1000L); // Give some starting points for testing
                wallet.setFrozenPoints(0L);
                walletRepository.save(wallet);
            }
            System.out.println("Seeded user: " + email + " with role: " + role);
        }
    }
}
