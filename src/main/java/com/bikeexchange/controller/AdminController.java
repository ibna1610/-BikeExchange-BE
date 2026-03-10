package com.bikeexchange.controller;

import com.bikeexchange.dto.request.RegisterRequest;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.model.Report;
import com.bikeexchange.model.InspectionReport;
import com.bikeexchange.model.InspectionRequest;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.UserWalletRepository;
import com.bikeexchange.repository.InspectionRepository;
import com.bikeexchange.service.service.AdminService;
import com.bikeexchange.service.service.WalletService;
import com.bikeexchange.service.UserReportService;
import com.bikeexchange.service.service.InspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin metrics", description = "Retrieve metrics by type: system, inspection, or reports")
    public ResponseEntity<?> getMetrics(
            @Parameter(example = "system") @RequestParam(required = false) String type) {
        Map<String, Object> data = new HashMap<>();
        
        if (type == null || type.isEmpty() || "system".equalsIgnoreCase(type)) {
            data.put("system", adminService.getDashboardMetrics());
        }
        if (type == null || type.isEmpty() || "inspection".equalsIgnoreCase(type)) {
            data.put("inspection", adminService.getInspectionMetrics());
        }
        if (type == null || type.isEmpty() || "reports".equalsIgnoreCase(type)) {
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("metrics", adminService.getReportMetrics());
            reportData.put("count", adminService.getReportsCount());
            reportData.put("pendingCount", adminService.getPendingReportsCount());
            data.put("reports", reportData);
        }

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all withdrawals", description = "Retrieve a list of all withdrawal requests with optional status filter.")
    public ResponseEntity<?> getWithdrawals(
            @Parameter(example = "PENDING") @RequestParam(name = "status", required = false) List<String> status) {

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
            @Parameter(example = "1") @PathVariable(name = "transactionId") Long transactionId) {
        walletService.approveWithdrawal(transactionId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Withdrawal approved and completed"));
    }

    @PostMapping("/withdrawals/{transactionId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a withdrawal", description = "Marks a pending withdrawal as FAILED and refunds points to caller.")
    public ResponseEntity<?> rejectWithdrawal(
            @Parameter(example = "1") @PathVariable(name = "transactionId") Long transactionId,
            @Parameter(example = "Invalid bank details") @RequestParam(name = "reason") String reason) {
        walletService.rejectWithdrawal(transactionId, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Withdrawal rejected and points refunded"));
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserReportService userReportService;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users", description = "List all users with optional filtering by role or search by email")
    public ResponseEntity<?> listUsers(
            @Parameter(example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(example = "BUYER") @RequestParam(required = false) String role,
            @Parameter(example = "user@example.com") @RequestParam(required = false) String search) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        // If search is provided, search by email
        if (search != null && !search.isBlank()) {
            var result = userRepository.findByEmailContainingIgnoreCase(search, pageable);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        }
        
        // If role is provided, filter by role
        if (role != null && !role.isBlank()) {
            try {
                User.UserRole r = User.UserRole.valueOf(role.toUpperCase());
                var result = userRepository.findByRole(r, pageable);
                return ResponseEntity.ok(Map.of("success", true, "data", result));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid role"));
            }
        }
        
        // Default: list all
        var result = userRepository.findAll(pageable);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user profile", description = "Update user status and/or role")
    public ResponseEntity<?> updateUser(
            @Parameter(example = "1") @PathVariable Long userId,
            @Parameter(example = "ACTIVE") @RequestParam(required = false) String status,
            @Parameter(example = "SELLER") @RequestParam(required = false) String role,
            @Parameter(example = "Suspicious activity") @RequestParam(required = false) String reason) {
        
        return userRepository.findById(userId).map(u -> {
            // Update status if provided
            if (status != null && !status.isBlank()) {
                u.setStatus(status);
            }
            
            // Update role if provided
            if (role != null && !role.isBlank()) {
                try {
                    u.setRole(User.UserRole.valueOf(role.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid role"));
                }
            }
            
            userRepository.save(u);
            Map<String, Object> response = Map.of("success", true, "data", u);
            return ResponseEntity.ok(response);
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
    private com.bikeexchange.service.service.PostService postService;

    @GetMapping("/listings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all listings", description = "Retrieve bike listings with optional status filter and pagination")
    public ResponseEntity<?> listListings(
            @Parameter(example = "APPROVED") @RequestParam(required = false) List<String> status,
            @Parameter(example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = postService.listPosts(null, status, pageable);
        return ResponseEntity.ok(
                Map.of("success", true, "data", result.map(com.bikeexchange.dto.response.PostResponse::fromEntity)));
    }

    @PutMapping("/listings/{postId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update listing status", description = "Approve, reject, or update listing status")
    public ResponseEntity<?> updateListing(
            @Parameter(example = "1") @PathVariable Long postId,
            @Parameter(example = "APPROVED") @RequestParam String action,
            @Parameter(example = "Bike condition is excellent") @RequestParam(required = false) String reason,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        
        Post post;
        
        if ("APPROVE".equalsIgnoreCase(action)) {
            post = postService.adminApprovePost(postId, currentUser.getId());
        } else if ("REJECT".equalsIgnoreCase(action)) {
            post = postService.adminRejectPost(postId, currentUser.getId(), reason);
        } else {
            // Generic status update
            post = postService.adminUpdatePostStatus(postId, currentUser.getId(), action, reason);
        }
        
        return ResponseEntity.ok(Map.of("success", true, "message", "Listing updated", "data", post));
    }

    @DeleteMapping("/listings/{postId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete listing", description = "Soft delete a bike listing")
    public ResponseEntity<?> deleteListing(
            @Parameter(example = "1") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Post post = postService.adminRejectPost(postId, currentUser.getId(), "deleted by admin");
        return ResponseEntity.ok(Map.of("success", true, "message", "Post deleted", "data", post));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List transactions", description = "Retrieve transactions with optional status filter")
    public ResponseEntity<?> listTransactions(
            @Parameter(example = "PENDING") @RequestParam(required = false) List<String> status) {
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
        var txs = walletService.getAllTransactions(statuses);
        List<PointTransactionDto> dtos = txs.stream().map(PointTransactionDto::from).toList();
        return ResponseEntity.ok(Map.of("success", true, "data", dtos));
    }

    @PutMapping("/transactions/{transactionId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelTransaction(@PathVariable Long transactionId,
            @RequestParam(required = false) String reason) {
        walletService.cancelTransaction(transactionId, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Transaction cancelled"));
    }

    // --- report management ---
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List reports", description = "List reports with optional filtering by type, status (PENDING), and pagination")
    public ResponseEntity<?> listReports(
            @Parameter(example = "USER_REPORT") @RequestParam(required = false) String type,
            @Parameter(example = "true") @RequestParam(required = false) Boolean pending,
            @Parameter(example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        // If pending flag is true, get pending reports
        if (pending != null && pending) {
            var result = userReportService.listPendingReports(pageable);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        }
        
        // If type is provided, filter by type
        if (type != null && !type.isBlank()) {
            try {
                Report.ReportType t = Report.ReportType.valueOf(type.toUpperCase());
                var result = userReportService.listByType(t, pageable);
                return ResponseEntity.ok(Map.of("success", true, "data", result));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid type"));
            }
        }
        
        // Default: list pending reports
        var result = userReportService.listPendingReports(pageable);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PutMapping("/reports/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve report", description = "Update report resolution status and add admin notes")
    public ResponseEntity<?> resolveReport(
            @Parameter(example = "1") @PathVariable Long reportId,
            @Parameter(example = "APPROVED") @RequestParam String resolution,
            @Parameter(example = "Issue verified") @RequestParam(required = false) String adminNote) {
        try {
            Report.ReportStatus status = Report.ReportStatus.valueOf(resolution.toUpperCase());
            Report updated = userReportService.resolveReport(reportId, status, adminNote);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid resolution"));
        }
    }

    // --- inspection management ---
    @GetMapping("/inspections/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listPendingInspections(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = inspectionRepository.findByStatus(InspectionRequest.RequestStatus.INSPECTED, pageable);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PutMapping("/inspections/{inspectionId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectInspection(@PathVariable Long inspectionId,
            @RequestParam(required = false) String reason,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        InspectionReport report = inspectionService.adminRejectInspection(inspectionId, currentUser.getId(), reason);
        return ResponseEntity.ok(Map.of("success", true, "data", report));
    }
}
