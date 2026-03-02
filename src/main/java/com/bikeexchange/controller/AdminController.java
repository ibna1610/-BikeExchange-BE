package com.bikeexchange.controller;

import com.bikeexchange.dto.request.RegisterRequest;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.UserWalletRepository;
import com.bikeexchange.service.AdminService;
import com.bikeexchange.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.bikeexchange.model.PointTransaction;
import com.bikeexchange.dto.response.PointTransactionDto;
import com.bikeexchange.model.Post;
import com.bikeexchange.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Management", description = "APIs for admin dashboard and management")
@SecurityRequirement(name = "Bearer Token")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    WalletService walletService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDashboardMetrics() {
        Map<String, Object> metrics = adminService.getDashboardMetrics();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", metrics);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all withdrawals", description = "Retrieve a list of all withdrawal requests with optional status filter.")
    public ResponseEntity<?> getWithdrawals(
            @Parameter(description = "Filter by statuses (PENDING, SUCCESS, FAILED). Repeat param or comma-separated.", example = "PENDING") @RequestParam(required = false) List<String> status) {

        List<PointTransaction.TransactionStatus> statuses = null;
        if (status != null && !status.isEmpty()) {
            statuses = status.stream()
                    .map(s -> {
                        try {
                            return PointTransaction.TransactionStatus.valueOf(s.trim().toUpperCase());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        var withdrawals = walletService.getWithdrawals(statuses);
        List<PointTransactionDto> dtos = withdrawals.stream()
                .map(PointTransactionDto::from)
                .toList();

        return ResponseEntity.ok(Map.of("success", true, "data", dtos));
    }

    @PostMapping("/withdrawals/{transactionId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a withdrawal", description = "Marks a pending withdrawal as SUCCESS and releases frozen points.")
    public ResponseEntity<?> approveWithdrawal(
            @Parameter(description = "ID of the withdrawal transaction", example = "1") @PathVariable Long transactionId) {
        walletService.approveWithdrawal(transactionId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Withdrawal approved and completed"));
    }

    @PostMapping("/withdrawals/{transactionId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a withdrawal", description = "Marks a pending withdrawal as FAILED and refunds points to caller.")
    public ResponseEntity<?> rejectWithdrawal(
            @Parameter(description = "ID of the withdrawal transaction", example = "1") @PathVariable Long transactionId,
            @Parameter(description = "Reason for rejection", example = "Invalid bank details") @RequestParam String reason) {
        walletService.rejectWithdrawal(transactionId, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Withdrawal rejected and points refunded"));
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- user management actions ---

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listUsers() {
        return ResponseEntity.ok(Map.of("success", true, "data", userRepository.findAll()));
    }

    @PostMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeUserRole(@PathVariable Long userId, @RequestParam String role) {
        return userRepository.findById(userId).map(u -> {
            try {
                u.setRole(com.bikeexchange.model.User.UserRole.valueOf(role.toUpperCase()));
                userRepository.save(u);
                return ResponseEntity.ok(Map.of("success", true, "data", u));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid role"));
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeUserStatus(@PathVariable Long userId, @RequestParam String status) {
        return userRepository.findById(userId).map(u -> {
            u.setStatus(status);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("success", true, "data", u));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/inspectors/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Admin: Create a new Inspector", description = "Allows an Admin to manually create an inspector account.")
    public ResponseEntity<?> createInspector(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already taken!"));
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

        // Auto-create wallet
        UserWallet wallet = new UserWallet();
        wallet.setUser(saved);
        wallet.setAvailablePoints(0L);
        wallet.setFrozenPoints(0L);
        walletRepository.save(wallet);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "message", "Inspector account created successfully", "data", saved));
    }

    @Autowired
    private com.bikeexchange.service.PostService postService;

    @PostMapping("/posts/{postId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approvePost(@PathVariable Long postId, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Post post = postService.adminApprovePost(postId, currentUser.getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Post approved", "data", post));
    }

    @PostMapping("/posts/{postId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectPost(@PathVariable Long postId, @RequestParam(required = false) String reason, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Post post = postService.adminRejectPost(postId, currentUser.getId(), reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Post rejected", "data", post));
    }
}
