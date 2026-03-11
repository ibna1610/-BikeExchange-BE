package com.bikeexchange.controller;

import com.bikeexchange.dto.request.RegisterRequest;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.UserWalletRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "6.1 Quản lý người dùng")
@SecurityRequirement(name = "Bearer Token")
public class AdminUserController extends AdminBaseController {

    @Autowired private UserRepository userRepository;
    @Autowired private UserWalletRepository walletRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    @Operation(summary = "Danh sách người dùng")
    public ResponseEntity<?> listUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        if (search != null && !search.isBlank()) {
            return ok("Users retrieved successfully", userRepository.findByEmailContainingIgnoreCase(search, pageable));
        }
        if (role != null && !role.isBlank()) {
            try {
                return ok("Users retrieved successfully",
                        userRepository.findByRole(User.UserRole.valueOf(role.toUpperCase()), pageable));
            } catch (IllegalArgumentException e) {
                return badRequest("Invalid role");
            }
        }
        return ok("Users retrieved successfully", userRepository.findAll(pageable));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Chi tiết người dùng")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(u -> ok("User retrieved successfully", u))
                .orElseGet(() -> notFound("User not found"));
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "Cập nhật thông tin / role")
    public ResponseEntity<?> updateUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String reason) {
        return userRepository.findById(userId).map(u -> {
            if (status != null && !status.isBlank()) u.setStatus(status);
            if (role != null && !role.isBlank()) {
                try {
                    u.setRole(User.UserRole.valueOf(role.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return badRequest("Invalid role");
                }
            }
            return ok("User updated successfully", userRepository.save(u));
        }).orElseGet(() -> notFound("User not found"));
    }

    @PutMapping("/users/{userId}/lock")
    @Operation(summary = "Khóa tài khoản")
    public ResponseEntity<?> lockUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        String reasonValue = (reason == null || reason.isBlank()) ? "Locked by admin" : reason;
        return userRepository.findById(userId).<ResponseEntity<?>>map(u -> {
            u.setStatus("LOCKED");
            userRepository.save(u);
            return ok("User locked", Map.of("userId", userId, "reason", reasonValue));
        }).orElseGet(() -> notFound("User not found"));
    }

    @PutMapping("/users/{userId}/unlock")
    @Operation(summary = "Mở khóa tài khoản")
    public ResponseEntity<?> unlockUser(@PathVariable Long userId) {
        return userRepository.findById(userId).<ResponseEntity<?>>map(u -> {
            u.setStatus("ACTIVE");
            return ok("User unlocked", userRepository.save(u));
        }).orElseGet(() -> notFound("User not found"));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Xóa người dùng")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        String reasonValue = (reason == null || reason.isBlank()) ? "Deleted by admin" : reason;
        return userRepository.findById(userId).<ResponseEntity<?>>map(u -> {
            u.setStatus("DELETED");
            userRepository.save(u);
            return ok("User deleted", Map.of("userId", userId, "reason", reasonValue));
        }).orElseGet(() -> notFound("User not found"));
    }

    @PostMapping("/inspectors/create")
    @Transactional
    @Operation(summary = "Tạo tài khoản inspector")
    public ResponseEntity<?> createInspector(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return badRequest("Email is already taken!");
        }
        User inspector = new User();
        inspector.setEmail(request.getEmail());
        inspector.setPassword(passwordEncoder.encode(request.getPassword()));
        inspector.setFullName(request.getFullName());
        inspector.setPhone(request.getPhone());
        inspector.setAddress(request.getAddress());
        inspector.setRole(User.UserRole.INSPECTOR);
        inspector.setIsVerified(true);
        inspector.setStatus("ACTIVE");
        User saved = userRepository.save(inspector);

        UserWallet wallet = new UserWallet();
        wallet.setUser(saved);
        wallet.setAvailablePoints(0L);
        wallet.setFrozenPoints(0L);
        walletRepository.save(wallet);

        return created("Inspector account created successfully", saved);
    }
}
