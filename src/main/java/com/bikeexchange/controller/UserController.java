package com.bikeexchange.controller;

import com.bikeexchange.model.User;
import com.bikeexchange.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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

    public record UserStats(Integer totalBikesSold, Double rating, Boolean isVerified) {
    }
}
