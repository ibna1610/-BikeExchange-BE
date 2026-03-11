package com.bikeexchange.controller;

import com.bikeexchange.dto.request.RegisterRequest;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.model.Report;
import com.bikeexchange.model.InspectionReport;
import com.bikeexchange.model.InspectionRequest;
import com.bikeexchange.model.Brand;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Order;
import com.bikeexchange.model.Component;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.UserWalletRepository;
import com.bikeexchange.repository.InspectionRepository;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.BrandRepository;
import com.bikeexchange.repository.CategoryRepository;
import com.bikeexchange.repository.ComponentRepository;
import com.bikeexchange.repository.OrderRepository;
import com.bikeexchange.repository.UserReportRepository;
import com.bikeexchange.repository.ReportRepository;
import com.bikeexchange.service.service.AdminService;
import com.bikeexchange.service.service.WalletService;
import com.bikeexchange.service.UserReportService;
import com.bikeexchange.service.service.InspectionService;
import com.bikeexchange.dto.response.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
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

        return ok("Metrics retrieved successfully", data);
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

        return ok("Withdrawals retrieved successfully", dtos);
    }

    @PostMapping("/withdrawals/{transactionId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a withdrawal", description = "Marks a pending withdrawal as SUCCESS and releases frozen points.")
    public ResponseEntity<?> approveWithdrawal(
            @Parameter(example = "1") @PathVariable(name = "transactionId") Long transactionId) {
        walletService.approveWithdrawal(transactionId);
        return ok("Withdrawal approved and completed", null);
    }

    @PostMapping("/withdrawals/{transactionId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a withdrawal", description = "Marks a pending withdrawal as FAILED and refunds points to caller.")
    public ResponseEntity<?> rejectWithdrawal(
            @Parameter(example = "1") @PathVariable(name = "transactionId") Long transactionId,
            @Parameter(example = "Invalid bank details") @RequestParam(name = "reason") String reason) {
        walletService.rejectWithdrawal(transactionId, reason);
        return ok("Withdrawal rejected and points refunded", null);
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
    private BikeRepository bikeRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserReportRepository userReportRepository;

    @Autowired
    private ReportRepository reportRepository;

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
            return ok("Users retrieved successfully", result);
        }
        
        // If role is provided, filter by role
        if (role != null && !role.isBlank()) {
            try {
                User.UserRole r = User.UserRole.valueOf(role.toUpperCase());
                var result = userRepository.findByRole(r, pageable);
                return ok("Users retrieved successfully", result);
            } catch (IllegalArgumentException ex) {
                return badRequest("Invalid role");
            }
        }
        
        // Default: list all
        var result = userRepository.findAll(pageable);
        return ok("Users retrieved successfully", result);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user detail", description = "Get a user detail by id")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(user -> ok("User retrieved successfully", user))
                .orElseGet(() -> notFound("User not found"));
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
                    return badRequest("Invalid role");
                }
            }
            
            userRepository.save(u);
            return ok("User updated successfully", u);
        }).orElseGet(() -> notFound("User not found"));
    }

    @PutMapping("/users/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lock user", description = "Lock user account")
    public ResponseEntity<?> lockUser(@PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "Locked by admin") String reason) {
        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(u -> {
                    u.setStatus("LOCKED");
                    userRepository.save(u);
                    Map<String, Object> data = new HashMap<>();
                    data.put("user", u);
                    data.put("reason", reason);
                    return ok("User locked", data);
                })
                .orElseGet(() -> notFound("User not found"));
    }

    @PutMapping("/users/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock user", description = "Unlock user account")
    public ResponseEntity<?> unlockUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(u -> {
                    u.setStatus("ACTIVE");
                    userRepository.save(u);
                    return ok("User unlocked", u);
                })
                .orElseGet(() -> notFound("User not found"));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Soft delete user by setting status DELETED")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "Deleted by admin") String reason) {
        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(u -> {
                    u.setStatus("DELETED");
                    userRepository.save(u);
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", userId);
                    data.put("reason", reason);
                    return ok("User deleted", data);
                })
                .orElseGet(() -> notFound("User not found"));
    }

    @PostMapping("/inspectors/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Admin: Create a new Inspector", description = "Allows an Admin to manually create an inspector account.")
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

        // Auto-create wallet
        UserWallet wallet = new UserWallet();
        wallet.setUser(saved);
        wallet.setAvailablePoints(0L);
        wallet.setFrozenPoints(0L);
        walletRepository.save(wallet);

        return created("Inspector account created successfully", saved);
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
        return ok("Listings retrieved successfully", result.map(com.bikeexchange.dto.response.PostResponse::fromEntity));
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
        
        return ok("Listing updated", post);
    }

    @DeleteMapping("/listings/{postId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete listing", description = "Soft delete a bike listing")
    public ResponseEntity<?> deleteListing(
            @Parameter(example = "1") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Post post = postService.adminRejectPost(postId, currentUser.getId(), "deleted by admin");
        return ok("Post deleted", post);
    }

    @GetMapping("/bikes/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List pending bikes", description = "Alias admin endpoint for bike moderation")
    public ResponseEntity<?> listPendingBikes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bike> result = bikeRepository.findByStatus(Bike.BikeStatus.DRAFT, pageable);
        return ok("Pending bikes retrieved successfully", result);
    }

    @GetMapping("/bikes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get bike detail", description = "Get bike detail by id for moderation")
    public ResponseEntity<?> getBikeById(@PathVariable Long id) {
        return bikeRepository.findById(id)
                .<ResponseEntity<?>>map(bike -> ok("Bike retrieved successfully", bike))
                .orElseGet(() -> notFound("Bike not found"));
    }

    @PutMapping("/bikes/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve bike", description = "Approve bike listing by setting ACTIVE status")
    public ResponseEntity<?> approveBike(@PathVariable Long id) {
        return bikeRepository.findById(id)
                .<ResponseEntity<?>>map(bike -> {
                    bike.setStatus(Bike.BikeStatus.ACTIVE);
                    bikeRepository.save(bike);
                    return ok("Bike approved", bike);
                })
                .orElseGet(() -> notFound("Bike not found"));
    }

    @PutMapping("/bikes/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject bike", description = "Reject bike listing by setting CANCELLED status")
    public ResponseEntity<?> rejectBike(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Rejected by admin") String reason) {
        return bikeRepository.findById(id)
                .<ResponseEntity<?>>map(bike -> {
                    bike.setStatus(Bike.BikeStatus.CANCELLED);
                    bikeRepository.save(bike);
                    Map<String, Object> data = new HashMap<>();
                    data.put("bike", bike);
                    data.put("reason", reason);
                    return ok("Bike rejected", data);
                })
                .orElseGet(() -> notFound("Bike not found"));
    }

    @PutMapping("/bikes/{id}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hide bike", description = "Hide bike listing by setting CANCELLED status")
    public ResponseEntity<?> hideBike(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Hidden by admin") String reason) {
        return rejectBike(id, reason);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List categories", description = "Admin categories list endpoint")
    public ResponseEntity<?> adminListCategories() {
        return ok("Categories retrieved successfully", categoryRepository.findAll());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", description = "Admin create category endpoint")
    public ResponseEntity<?> adminCreateCategory(@RequestBody com.bikeexchange.model.Category payload) {
        if (payload.getName() == null || payload.getName().isBlank()) {
            return badRequest("Category name is required");
        }
        if (categoryRepository.findByName(payload.getName()).isPresent()) {
            return badRequest("Category already exists");
        }
        com.bikeexchange.model.Category saved = categoryRepository.save(payload);
        return created("Category created successfully", saved);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category", description = "Admin update category endpoint")
    public ResponseEntity<?> adminUpdateCategory(@PathVariable Long id, @RequestBody com.bikeexchange.model.Category payload) {
        return categoryRepository.findById(id)
                .<ResponseEntity<?>>map(cat -> {
                    if (payload.getName() != null && !payload.getName().isBlank()) {
                        cat.setName(payload.getName());
                    }
                    cat.setDescription(payload.getDescription());
                    cat.setImgUrl(payload.getImgUrl());
                    categoryRepository.save(cat);
                    return ok("Category updated successfully", cat);
                })
                .orElseGet(() -> notFound("Category not found"));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category", description = "Admin delete category endpoint")
    public ResponseEntity<?> adminDeleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return notFound("Category not found");
        }
        categoryRepository.deleteById(id);
        return ok("Category deleted", Map.of("id", id));
    }

    @GetMapping("/brands")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List brands", description = "Admin brands list endpoint")
    public ResponseEntity<?> adminListBrands() {
        return ok("Brands retrieved successfully", brandRepository.findAll());
    }

    @PostMapping("/brands")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create brand", description = "Admin create brand endpoint")
    public ResponseEntity<?> adminCreateBrand(@RequestBody Brand payload) {
        if (payload.getName() == null || payload.getName().isBlank()) {
            return badRequest("Brand name is required");
        }
        if (brandRepository.findByName(payload.getName()).isPresent()) {
            return badRequest("Brand already exists");
        }
        Brand saved = brandRepository.save(payload);
        return created("Brand created successfully", saved);
    }

    @PutMapping("/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update brand", description = "Admin update brand endpoint")
    public ResponseEntity<?> adminUpdateBrand(@PathVariable Long id, @RequestBody Brand payload) {
        return brandRepository.findById(id)
                .<ResponseEntity<?>>map(brand -> {
                    if (payload.getName() != null && !payload.getName().isBlank()) {
                        brand.setName(payload.getName());
                    }
                    brand.setDescription(payload.getDescription());
                    brandRepository.save(brand);
                    return ok("Brand updated successfully", brand);
                })
                .orElseGet(() -> notFound("Brand not found"));
    }

    @DeleteMapping("/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete brand", description = "Admin delete brand endpoint")
    public ResponseEntity<?> adminDeleteBrand(@PathVariable Long id) {
        if (!brandRepository.existsById(id)) {
            return notFound("Brand not found");
        }
        brandRepository.deleteById(id);
        return ok("Brand deleted", Map.of("id", id));
    }

    @GetMapping("/components")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List components", description = "Admin components list endpoint")
    public ResponseEntity<?> listComponents() {
        return ok("Components retrieved successfully", componentRepository.findAll());
    }

    @PostMapping("/components")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create component", description = "Admin create component endpoint")
    public ResponseEntity<?> createComponent(@RequestBody Component payload) {
        if (payload.getName() == null || payload.getName().isBlank()) {
            return badRequest("Component name is required");
        }
        if (componentRepository.findByName(payload.getName()).isPresent()) {
            return badRequest("Component already exists");
        }

        Component saved = componentRepository.save(payload);
        return created("Component created successfully", saved);
    }

    @PutMapping("/components/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update component", description = "Admin update component endpoint")
    public ResponseEntity<?> updateComponent(@PathVariable Long id, @RequestBody Component payload) {
        return componentRepository.findById(id)
                .<ResponseEntity<?>>map(component -> {
                    if (payload.getName() != null && !payload.getName().isBlank()) {
                        component.setName(payload.getName());
                    }
                    component.setDescription(payload.getDescription());
                    componentRepository.save(component);
                    return ok("Component updated successfully", component);
                })
                .orElseGet(() -> notFound("Component not found"));
    }

    @DeleteMapping("/components/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete component", description = "Admin delete component endpoint")
    public ResponseEntity<?> deleteComponent(@PathVariable Long id) {
        if (!componentRepository.existsById(id)) {
            return notFound("Component not found");
        }
        componentRepository.deleteById(id);
        return ok("Component deleted", Map.of("id", id));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List orders", description = "List all orders with optional status filter")
    public ResponseEntity<?> listOrders(@RequestParam(required = false) String status) {
        List<Order> orders;
        if (status == null || status.isBlank()) {
            orders = orderRepository.findAll();
        } else {
            try {
                Order.OrderStatus target = Order.OrderStatus.valueOf(status.trim().toUpperCase());
                orders = orderRepository.findAll().stream().filter(o -> o.getStatus() == target).toList();
            } catch (IllegalArgumentException ex) {
                return badRequest("Invalid order status");
            }
        }

        List<OrderResponse> data = orders.stream().map(OrderResponse::fromEntity).toList();
        return ResponseEntity.ok(Map.of("success", true, "message", "Orders retrieved successfully", "data", data, "summary", Map.of("total", data.size())));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get order detail", description = "Get one order by id")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .<ResponseEntity<?>>map(order -> ok("Order retrieved successfully", OrderResponse.fromEntity(order)))
                .orElseGet(() -> notFound("Order not found"));
    }

    @PutMapping("/orders/{id}/update-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status", description = "Admin update order status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason) {
        Order.OrderStatus targetStatus;
        try {
            targetStatus = Order.OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return badRequest("Invalid order status");
        }

        return orderRepository.findById(id)
                .<ResponseEntity<?>>map(order -> {
                    order.setStatus(targetStatus);
                    Order saved = orderRepository.save(order);
                    Map<String, Object> data = new HashMap<>();
                    data.put("order", OrderResponse.fromEntity(saved));
                    data.put("reason", reason);
                    return ok("Order status updated", data);
                })
                .orElseGet(() -> notFound("Order not found"));
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
        return ok("Transactions retrieved successfully", dtos);
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List payments", description = "Alias endpoint mapped to admin transactions")
    public ResponseEntity<?> listPayments(
            @Parameter(example = "PENDING") @RequestParam(required = false) List<String> status) {
        return listTransactions(status);
    }

    @PutMapping("/transactions/{transactionId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelTransaction(@PathVariable Long transactionId,
            @RequestParam(required = false) String reason) {
        walletService.cancelTransaction(transactionId, reason);
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", transactionId);
        data.put("reason", reason);
        return ok("Transaction cancelled", data);
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
            return ok("Reports retrieved successfully", result);
        }
        
        // If type is provided, filter by type
        if (type != null && !type.isBlank()) {
            try {
                Report.ReportType t = Report.ReportType.valueOf(type.toUpperCase());
                var result = userReportService.listByType(t, pageable);
                return ok("Reports retrieved successfully", result);
            } catch (IllegalArgumentException ex) {
                return badRequest("Invalid type");
            }
        }
        
        // Default: list pending reports
        var result = userReportService.listPendingReports(pageable);
        return ok("Reports retrieved successfully", result);
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
            return ok("Report resolved successfully", updated);
        } catch (IllegalArgumentException ex) {
            return badRequest("Invalid resolution");
        }
    }

    @PutMapping("/reports/{reportId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process report", description = "Mark report as REVIEWING and update admin note")
    public ResponseEntity<?> processReport(
            @PathVariable Long reportId,
            @RequestParam(required = false) String adminNote) {
        try {
            Report updated = userReportService.resolveReport(reportId, Report.ReportStatus.REVIEWING, adminNote);
            return ok("Report is now being processed", updated);
        } catch (IllegalArgumentException ex) {
            return notFound("Report not found");
        }
    }

    @PutMapping("/reports/{reportId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve report", description = "Close report with RESOLVED or REJECTED status")
    public ResponseEntity<?> resolveReportByPath(
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "RESOLVED") String resolution,
            @RequestParam(required = false) String adminNote) {
        try {
            Report.ReportStatus status = Report.ReportStatus.valueOf(resolution.trim().toUpperCase());
            if (status != Report.ReportStatus.RESOLVED && status != Report.ReportStatus.REJECTED) {
                return badRequest("resolution must be RESOLVED or REJECTED");
            }
            Report updated = userReportService.resolveReport(reportId, status, adminNote);
            return ok("Report resolved successfully", updated);
        } catch (IllegalArgumentException ex) {
            if ("Report not found".equals(ex.getMessage())) {
                return notFound("Report not found");
            }
            return badRequest("Invalid resolution");
        }
    }

    @GetMapping("/reports/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get report detail", description = "Get report detail by id")
    public ResponseEntity<?> getReportById(@PathVariable Long reportId) {
        return userReportRepository.findById(reportId)
            .<ResponseEntity<?>>map(report -> ok("Report retrieved successfully", report))
            .orElseGet(() -> notFound("Report not found"));
    }

    // --- inspection management ---
    @GetMapping("/inspections/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listPendingInspections(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = inspectionRepository.findByStatus(InspectionRequest.RequestStatus.INSPECTED, pageable);
        return ok("Pending inspections retrieved successfully", result);
    }

    @PutMapping("/inspections/{inspectionId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectInspection(@PathVariable Long inspectionId,
            @RequestParam(required = false) String reason,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        InspectionReport report = inspectionService.adminRejectInspection(inspectionId, currentUser.getId(), reason);
        return ok("Inspection rejected", report);
    }

    @GetMapping("/inspection-requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List inspection requests", description = "List inspection requests with optional status filter")
    public ResponseEntity<?> listInspectionRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status == null || status.isBlank()) {
            return ok("Inspection requests retrieved successfully", inspectionRepository.findAll(pageable));
        }

        try {
            InspectionRequest.RequestStatus s = InspectionRequest.RequestStatus.valueOf(status.trim().toUpperCase());
            return ok("Inspection requests retrieved successfully", inspectionRepository.findByStatus(s, pageable));
        } catch (IllegalArgumentException ex) {
            return badRequest("Invalid inspection status");
        }
    }

    @GetMapping("/inspection-reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List inspection reports", description = "List approved/rejected inspection reports")
    public ResponseEntity<?> listInspectionReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ok("Inspection reports retrieved successfully", reportRepository.findAll(pageable));
    }

    @GetMapping("/inspectors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List inspectors", description = "List all inspector accounts")
    public ResponseEntity<?> listInspectors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ok("Inspectors retrieved successfully", userRepository.findByRole(User.UserRole.INSPECTOR, pageable));
    }

    @PutMapping("/inspectors/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve inspector", description = "Approve inspector account")
    public ResponseEntity<?> approveInspector(@PathVariable Long id) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(user -> {
                    if (user.getRole() != User.UserRole.INSPECTOR) {
                        return badRequest("User is not an inspector");
                    }
                    user.setStatus("ACTIVE");
                    user.setIsVerified(true);
                    userRepository.save(user);
                    return ok("Inspector approved", user);
                })
                .orElseGet(() -> notFound("Inspector not found"));
    }

    @PutMapping("/inspectors/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend inspector", description = "Suspend inspector account")
    public ResponseEntity<?> suspendInspector(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Suspended by admin") String reason) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(user -> {
                    if (user.getRole() != User.UserRole.INSPECTOR) {
                        return badRequest("User is not an inspector");
                    }
                    user.setStatus("SUSPENDED");
                    userRepository.save(user);
                    Map<String, Object> data = new HashMap<>();
                    data.put("inspector", user);
                    data.put("reason", reason);
                    return ok("Inspector suspended", data);
                })
                .orElseGet(() -> notFound("Inspector not found"));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin dashboard", description = "Get dashboard overview")
    public ResponseEntity<?> dashboard() {
        return ok("Dashboard retrieved successfully", adminService.getDashboardMetrics());
    }

    @GetMapping("/statistics/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "User statistics", description = "Get user statistics by role")
    public ResponseEntity<?> userStatistics() {
        List<User> users = userRepository.findAll();
        Map<String, Long> byRole = new HashMap<>();
        for (User user : users) {
            String role = user.getRole() != null ? user.getRole().name() : "UNKNOWN";
            byRole.put(role, byRole.getOrDefault(role, 0L) + 1L);
        }
        return ok("User statistics retrieved successfully", Map.of("total", users.size(), "byRole", byRole));
    }

    @GetMapping("/statistics/bikes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bike statistics", description = "Get bike statistics by status")
    public ResponseEntity<?> bikeStatistics() {
        List<Bike> bikes = bikeRepository.findAll();
        Map<String, Long> byStatus = new HashMap<>();
        for (Bike bike : bikes) {
            String status = bike.getStatus() != null ? bike.getStatus().name() : "UNKNOWN";
            byStatus.put(status, byStatus.getOrDefault(status, 0L) + 1L);
        }
        return ok("Bike statistics retrieved successfully", Map.of("total", bikes.size(), "byStatus", byStatus));
    }

    @GetMapping("/statistics/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Order statistics", description = "Get order statistics by status")
    public ResponseEntity<?> orderStatistics() {
        List<Order> orders = orderRepository.findAll();
        Map<String, Long> byStatus = new HashMap<>();
        for (Order order : orders) {
            String status = order.getStatus() != null ? order.getStatus().name() : "UNKNOWN";
            byStatus.put(status, byStatus.getOrDefault(status, 0L) + 1L);
        }
        return ok("Order statistics retrieved successfully", Map.of("total", orders.size(), "byStatus", byStatus));
    }

    @GetMapping("/statistics/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Revenue statistics", description = "Get system revenue statistics")
    public ResponseEntity<?> revenueStatistics() {
        Map<String, Object> dashboard = adminService.getDashboardMetrics();
        Object revenue = dashboard.getOrDefault("totalCommissionRevenue", 0L);
        Object completedOrders = dashboard.getOrDefault("totalCompletedOrders", 0L);
        return ok("Revenue statistics retrieved successfully", Map.of("totalCommissionRevenue", revenue, "completedOrders", completedOrders));
    }

    @GetMapping("/statistics/inspections")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inspection statistics", description = "Get inspection statistics by status")
    public ResponseEntity<?> inspectionStatistics() {
        return ok("Inspection statistics retrieved successfully", adminService.getInspectionMetrics());
    }

    @GetMapping("/fees")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "System fees", description = "Get platform fee overview based on completed orders")
    public ResponseEntity<?> listSystemFees() {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == Order.OrderStatus.COMPLETED)
                .toList();

        long totalCommission = 0L;
        List<Map<String, Object>> rows = orders.stream().map(order -> {
            long amount = order.getAmountPoints() != null ? order.getAmountPoints() : 0L;
            long fee = Math.round(amount * 0.05);
            Map<String, Object> row = new HashMap<>();
            row.put("orderId", order.getId());
            row.put("amountPoints", amount);
            row.put("commissionRate", 0.05);
            row.put("commissionFee", fee);
            return row;
        }).toList();

        for (Map<String, Object> row : rows) {
            totalCommission += (Long) row.get("commissionFee");
        }

        return ResponseEntity.ok(Map.of("success", true,
                "message", "System fees retrieved successfully",
                "data", rows,
                "summary", Map.of("totalCompletedOrders", rows.size(), "totalCommissionFee", totalCommission)));
    }

    private ResponseEntity<?> ok(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> created(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", message));
    }

    private ResponseEntity<?> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", message));
    }

    private ResponseEntity<?> notImplemented(String message) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("success", false, "message", message));
    }
}
