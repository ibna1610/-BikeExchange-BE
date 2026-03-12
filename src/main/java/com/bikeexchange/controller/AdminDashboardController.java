package com.bikeexchange.controller;

import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Order;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.OrderRepository;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.service.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Dashboard", description = "6.7 Dashboard / thống kê / báo cáo")
@SecurityRequirement(name = "Bearer Token")
public class AdminDashboardController extends AdminBaseController {

    @Autowired private AdminService adminService;
    @Autowired private UserRepository userRepository;
    @Autowired private BikeRepository bikeRepository;
    @Autowired private OrderRepository orderRepository;

    @GetMapping("/dashboard")
    @Operation(summary = "Tổng quan hệ thống")
    public ResponseEntity<?> dashboard() {
        return ok("Dashboard retrieved successfully", adminService.getDashboardMetrics());
    }

    @GetMapping("/statistics/users")
    @Operation(summary = "Thống kê người dùng")
    public ResponseEntity<?> userStatistics() {
        List<User> users = userRepository.findAll();
        Map<String, Long> byRole = new HashMap<>();
        for (User u : users) {
            String r = u.getRole() != null ? u.getRole().name() : "UNKNOWN";
            byRole.put(r, byRole.getOrDefault(r, 0L) + 1L);
        }
        return ok("User statistics retrieved successfully",
                Map.of("total", users.size(), "byRole", byRole));
    }

    @GetMapping("/statistics/bikes")
    @Operation(summary = "Thống kê tin đăng")
    public ResponseEntity<?> bikeStatistics() {
        List<Bike> bikes = bikeRepository.findAll();
        Map<String, Long> byStatus = new HashMap<>();
        for (Bike b : bikes) {
            String s = b.getStatus() != null ? b.getStatus().name() : "UNKNOWN";
            byStatus.put(s, byStatus.getOrDefault(s, 0L) + 1L);
        }
        return ok("Bike statistics retrieved successfully",
                Map.of("total", bikes.size(), "byStatus", byStatus));
    }

    @GetMapping("/statistics/orders")
    @Operation(summary = "Thống kê giao dịch")
    public ResponseEntity<?> orderStatistics() {
        List<Order> orders = orderRepository.findAll();
        Map<String, Long> byStatus = new HashMap<>();
        for (Order o : orders) {
            String s = o.getStatus() != null ? o.getStatus().name() : "UNKNOWN";
            byStatus.put(s, byStatus.getOrDefault(s, 0L) + 1L);
        }
        return ok("Order statistics retrieved successfully",
                Map.of("total", orders.size(), "byStatus", byStatus));
    }

    @GetMapping("/statistics/revenue")
    @Operation(summary = "Thống kê doanh thu")
    public ResponseEntity<?> revenueStatistics() {
        Map<String, Object> dashboard = adminService.getDashboardMetrics();
        return ok("Revenue statistics retrieved successfully", Map.of(
                "totalCommissionRevenue", dashboard.getOrDefault("totalCommissionRevenue", 0L),
                "completedOrders", dashboard.getOrDefault("totalCompletedOrders", 0L)));
    }

    @GetMapping("/statistics/inspections")
    @Operation(summary = "Thống kê kiểm định")
    public ResponseEntity<?> inspectionStatistics() {
        return ok("Inspection statistics retrieved successfully", adminService.getInspectionMetrics());
    }

    @GetMapping("/metrics")
    @Operation(summary = "Tổng hợp metrics hệ thống")
    public ResponseEntity<?> getMetrics(@RequestParam(required = false) String type) {
        Map<String, Object> data = new HashMap<>();
        if (type == null || type.isEmpty() || "system".equalsIgnoreCase(type)) {
            data.put("system", adminService.getDashboardMetrics());
        }
        if (type == null || type.isEmpty() || "inspection".equalsIgnoreCase(type)) {
            data.put("inspection", adminService.getInspectionMetrics());
        }
        if (type == null || type.isEmpty() || "reports".equalsIgnoreCase(type)) {
            data.put("reports", Map.of(
                    "metrics", adminService.getReportMetrics(),
                    "count", adminService.getReportsCount(),
                    "pendingCount", adminService.getPendingReportsCount()));
        }
        return ok("Metrics retrieved successfully", data);
    }
}
