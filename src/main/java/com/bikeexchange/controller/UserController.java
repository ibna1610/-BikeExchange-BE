package com.bikeexchange.controller;

import com.bikeexchange.model.User;
import com.bikeexchange.service.UserService;
import com.bikeexchange.dto.request.UpgradeToSellerRequest;
import com.bikeexchange.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "APIs for user CRUD operations and profile management")
@SecurityRequirement(name = "Bearer Token")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(userId, user);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{userId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> verifyUser(@PathVariable Long userId) {
        try {
            User verifiedUser = userService.verifyUser(userId);
            return ResponseEntity.ok(verifiedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable Long userId) {
        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(new UserStats(
                        user.getTotalBikesSold(),
                        user.getRating(),
                        user.getIsVerified())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/upgrade-to-seller")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upgrade user from Buyer to Seller", description = "Allows an authenticated buyer to upgrade their account to a seller account")
    public ResponseEntity<?> upgradeToSeller(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long userId,
            @RequestBody UpgradeToSellerRequest request) {
        try {
            // Verify that the user is upgrading their own account
            if (!currentUser.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "You can only upgrade your own account"));
            }

            // Verify that all required fields are provided
            if (request.getShopName() == null || request.getShopName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Shop name is required"));
            }

            if (request.getShopDescription() == null || request.getShopDescription().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Shop description is required"));
            }

            if (!Boolean.TRUE.equals(request.getAgreeToTerms())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "You must agree to the terms and conditions"));
            }

            User upgradedUser = userService.upgradeToSeller(userId, request.getShopName(), request.getShopDescription());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User successfully upgraded to seller");
            response.put("data", upgradedUser);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    public record UserStats(Integer totalBikesSold, Double rating, Boolean isVerified) {
    }}