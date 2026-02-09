package com.bikeexchange.controller;

import com.bikeexchange.model.*;
import com.bikeexchange.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Admin API - Chức năng Quản trị viên
 * Duyệt tin, quản lý user, xử lý báo cáo, kiốnéo quản lý giao dịch, thống kê
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Admin Management", description = "API cho chức năng Quản trị viên (Admin)")
public class AdminController {
    @Autowired
    private AdminService adminService;

    // ==================== LISTING MANAGEMENT ====================
    @PutMapping("/listings/{bikeId}/approve")
    @Operation(summary = "Duyệt tin đăng xe")
    @ApiResponse(responseCode = "200", description = "Duyệt thành công")
    public ResponseEntity<?> approveListing(
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId) {
        try {
            Bike bike = adminService.approveListing(bikeId);
            return ResponseEntity.ok(bike);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/listings/{bikeId}/reject")
    @Operation(summary = "Từ chối tin đăng xe",
            description = "Admin từ chối tin với lý do vi phạm")
    @ApiResponse(responseCode = "200", description = "Từ chối thành công")
    public ResponseEntity<?> rejectListing(
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId,
            @Parameter(description = "Lý do từ chối") @RequestParam String reason) {
        try {
            Bike bike = adminService.rejectListing(bikeId, reason);
            return ResponseEntity.ok(bike);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/listings/pending")
    @Operation(summary = "Xem tin đăng chờ duyệt")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<?> getPendingListings(
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử trên trang") @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Bike> listings = adminService.getPendingListings(pageable);
            return ResponseEntity.ok(listings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/listings/{bikeId}/lock")
    @Operation(summary = "Khóa tin đăng")
    @ApiResponse(responseCode = "200", description = "Khóa thành công")
    public ResponseEntity<?> lockListing(
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId) {
        try {
            Bike bike = adminService.lockListing(bikeId);
            return ResponseEntity.ok(bike);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/listings/{bikeId}")
    @Operation(summary = "Xóa tin đăng")
    @ApiResponse(responseCode = "200", description = "Xóa thành công")
    public ResponseEntity<?> deleteListing(
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId) {
        try {
            adminService.deleteListing(bikeId);
            return ResponseEntity.ok(Map.of("message", "Listing deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== USER MANAGEMENT ====================
    @GetMapping("/users")
    @Operation(summary = "Xem danh sách tất cả người dùng")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<?> getAllUsers(
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử trên trang") @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = adminService.getAllUsers(pageable);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users/role/{role}")
    @Operation(summary = "Xem danh sách người dùng theo role")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<?> getUsersByRole(
            @Parameter(description = "Role (BUYER, SELLER, ADMIN)") @PathVariable User.UserRole role,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử trên trang") @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = adminService.getUsersByRole(role, pageable);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/activate")
    @Operation(summary = "Kích hoạt tài khoản")
    @ApiResponse(responseCode = "200", description = "Kích hoạt thành công")
    public ResponseEntity<?> activateUser(
            @Parameter(description = "ID của User") @PathVariable Long userId) {
        try {
            User user = adminService.activateUser(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/deactivate")
    @Operation(summary = "Vô hiệu hóa tài khoản")
    @ApiResponse(responseCode = "200", description = "Vô hiệu hóa thành công")
    public ResponseEntity<?> deactivateUser(
            @Parameter(description = "ID của User") @PathVariable Long userId) {
        try {
            User user = adminService.deactivateUser(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/suspend")
    @Operation(summary = "Khóa tài khoản người dùng",
            description = "Admin khóa tài khoản vì vi phạm chính sách")
    @ApiResponse(responseCode = "200", description = "Khóa thành công")
    public ResponseEntity<?> suspendUser(
            @Parameter(description = "ID của User") @PathVariable Long userId,
            @Parameter(description = "Lý do khóa") @RequestParam String reason) {
        try {
            User user = adminService.suspendUser(userId, reason);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users/search")
    @Operation(summary = "Tìm kiếm người dùng theo email")
    @ApiResponse(responseCode = "200", description = "Tìm kiếm thành công")
    public ResponseEntity<?> searchUsersByEmail(
            @Parameter(description = "Email cần tìm") @RequestParam String email) {
        try {
            List<User> users = adminService.getUsersByEmail(email);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== REPORT MANAGEMENT ====================
    @GetMapping("/reports/pending")
    @Operation(summary = "Xem báo cáo chờ xử lý")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách báo cáo thành công")
    public ResponseEntity<?> getPendingReports(
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử trên trang") @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Report> reports = adminService.getPendingReports(pageable);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/reports/{reportId}/resolve")
    @Operation(summary = "Xử lý báo cáo")
    @ApiResponse(responseCode = "200", description = "Xử lý thành công")
    public ResponseEntity<?> resolveReport(
            @Parameter(description = "ID của Report") @PathVariable Long reportId,
            @Parameter(description = "Ghi chú của Admin") @RequestParam String adminNote,
            @Parameter(description = "Kết luận") @RequestParam Report.ReportStatus resolution) {
        try {
            Report report = adminService.resolveReport(reportId, adminNote, resolution);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/reports/type/{type}")
    @Operation(summary = "Xem báo cáo theo loại")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách báo cáo thành công")
    public ResponseEntity<?> getReportsByType(
            @Parameter(description = "Loại báo cáo") @PathVariable Report.ReportType type) {
        try {
            List<Report> reports = adminService.getReportsByType(type);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== TRANSACTION MANAGEMENT ====================
    @GetMapping("/transactions")
    @Operation(summary = "Xem tất cả giao dịch")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách giao dịch thành công")
    public ResponseEntity<?> getAllTransactions(
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử trên trang") @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Transaction> transactions = adminService.getAllTransactions(pageable);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/transactions/status/{status}")
    @Operation(summary = "Xem giao dịch theo trạng thái")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách giao dịch thành công")
    public ResponseEntity<?> getTransactionsByStatus(
            @Parameter(description = "Trạng thái giao dịch") @PathVariable Transaction.TransactionStatus status,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử trên trang") @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Transaction> transactions = adminService.getTransactionsByStatus(status, pageable);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/transactions/{transactionId}/cancel")
    @Operation(summary = "Hủy giao dịch",
            description = "Admin có thể hủy giao dịch và hoàn tiền")
    @ApiResponse(responseCode = "200", description = "Hủy thành công")
    public ResponseEntity<?> cancelTransaction(
            @Parameter(description = "ID của Transaction") @PathVariable Long transactionId,
            @Parameter(description = "Lý do hủy") @RequestParam String reason) {
        try {
            Transaction transaction = adminService.cancelTransaction(transactionId, reason);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== INSPECTION MANAGEMENT ====================
    @PutMapping("/inspections/{inspectionId}/approve")
    @Operation(summary = "Duyệt báo cáo kiểm định")
    @ApiResponse(responseCode = "200", description = "Duyệt thành công")
    public ResponseEntity<?> approveInspection(
            @Parameter(description = "ID của Inspection") @PathVariable Long inspectionId) {
        try {
            Inspection inspection = adminService.approveInspection(inspectionId);
            return ResponseEntity.ok(inspection);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/inspections/{inspectionId}/reject")
    @Operation(summary = "Từ chối báo cáo kiểm định")
    @ApiResponse(responseCode = "200", description = "Từ chối thành công")
    public ResponseEntity<?> rejectInspection(
            @Parameter(description = "ID của Inspection") @PathVariable Long inspectionId,
            @Parameter(description = "Lý do từ chối") @RequestParam String reason) {
        try {
            Inspection inspection = adminService.rejectInspection(inspectionId, reason);
            return ResponseEntity.ok(inspection);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/inspections/pending")
    @Operation(summary = "Xem báo cáo kiểm định chờ duyệt")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách báo cáo thành công")
    public ResponseEntity<?> getPendingInspections(
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử trên trang") @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Inspection> inspections = adminService.getPendingInspections(pageable);
            return ResponseEntity.ok(inspections);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== STATISTICS & DASHBOARD ====================
    @GetMapping("/metrics/system")
    @Operation(summary = "Xem thống kê hệ thống")
    @ApiResponse(responseCode = "200", description = "Lấy thống kê thành công")
    public ResponseEntity<?> getSystemMetrics() {
        try {
            Map<String, Object> metrics = adminService.getSystemMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/metrics/dashboard")
    @Operation(summary = "Xem Dashboard")
    @ApiResponse(responseCode = "200", description = "Lấy dashboard statistics thành công")
    public ResponseEntity<?> getDashboardStats() {
        try {
            Map<String, Object> stats = adminService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/metrics/inspection")
    @Operation(summary = "Xem thống kê kiểm định")
    @ApiResponse(responseCode = "200", description = "Lấy thống kê kiểm định thành công")
    public ResponseEntity<?> getInspectionStats() {
        try {
            Map<String, Object> stats = adminService.getInspectionStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/metrics/reports")
    @Operation(summary = "Xem thống kê báo cáo")
    @ApiResponse(responseCode = "200", description = "Lấy thống kê báo cáo thành công")
    public ResponseEntity<?> getReportStats() {
        try {
            Map<String, Object> stats = adminService.getReportStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/metrics/reports-count")
    @Operation(summary = "Tổng số báo cáo")
    @ApiResponse(responseCode = "200", description = "Lấy thống kê thành công")
    public ResponseEntity<?> getTotalReportsCount() {
        try {
            Long count = adminService.getTotalReportsCount();
            return ResponseEntity.ok(Map.of("totalReports", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/metrics/pending-reports-count")
    @Operation(summary = "Số báo cáo chờ xử lý")
    @ApiResponse(responseCode = "200", description = "Lấy thống kê thành công")
    public ResponseEntity<?> getPendingReportsCount() {
        try {
            Long count = adminService.getPendingReportsCount();
            return ResponseEntity.ok(Map.of("pendingReports", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
