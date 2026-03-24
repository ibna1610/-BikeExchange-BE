package com.bikeexchange.controller;

import com.bikeexchange.dto.response.OrderResponse;
import com.bikeexchange.dto.response.PointTransactionDto;
import com.bikeexchange.dto.request.OrderRuleConfigRequest;
import com.bikeexchange.model.Order;
import com.bikeexchange.model.OrderRuleConfig;
import com.bikeexchange.model.PointTransaction;
import com.bikeexchange.repository.OrderRepository;
import com.bikeexchange.service.service.OrderRuleConfigService;
import com.bikeexchange.service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Orders & Payments", description = "6.4 Quản lý giao dịch")
@SecurityRequirement(name = "Bearer Token")
public class AdminOrderController extends AdminBaseController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private WalletService walletService;
    @Autowired private OrderRuleConfigService orderRuleConfigService;

    @GetMapping("/order-rules")
    @Operation(summary = "Xem cấu hình business rule của order")
    public ResponseEntity<?> getOrderRules() {
        OrderRuleConfig config = orderRuleConfigService.getCurrentRules();
        Map<String, Object> data = new HashMap<>();
        data.put("commissionRate", config.getCommissionRate());
        data.put("sellerUpgradeFee", config.getSellerUpgradeFee());
        data.put("returnWindowDays", config.getReturnWindowDays());
        data.put("bikePostFee", config.getBikePostFee());
        data.put("inspectionFee", config.getInspectionFee());
        return ok("Order rules retrieved successfully", data);
    }

    @PutMapping("/order-rules")
    @Operation(summary = "Cập nhật cấu hình business rule của order")
    public ResponseEntity<?> updateOrderRules(@RequestBody OrderRuleConfigRequest request) {
        try {
            OrderRuleConfig config = orderRuleConfigService.updateRules(
                    request.getCommissionRate(),
                    request.getSellerUpgradeFee(),
                    request.getReturnWindowDays(),
                    null,
                    null
            );
            Map<String, Object> data = new HashMap<>();
            data.put("commissionRate", config.getCommissionRate());
            data.put("sellerUpgradeFee", config.getSellerUpgradeFee());
            data.put("returnWindowDays", config.getReturnWindowDays());
            data.put("bikePostFee", config.getBikePostFee());
            data.put("inspectionFee", config.getInspectionFee());
            return ok("Order rules updated successfully", data);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @PutMapping("/order-rules/commission-rate")
    @Operation(summary = "Cập nhật tỷ lệ hoa hồng")
    public ResponseEntity<?> updateCommissionRate(@RequestParam Double commissionRate) {
        try {
            OrderRuleConfig config = orderRuleConfigService.updateRules(commissionRate, null, null, null, null);
            return ok("Commission rate updated successfully", Map.of("commissionRate", config.getCommissionRate()));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @PutMapping("/order-rules/seller-upgrade-fee")
    @Operation(summary = "Cập nhật phí nâng cấp người bán")
    public ResponseEntity<?> updateSellerUpgradeFee(@RequestParam Long sellerUpgradeFee) {
        try {
            OrderRuleConfig config = orderRuleConfigService.updateRules(null, sellerUpgradeFee, null, null, null);
            return ok("Seller upgrade fee updated successfully", Map.of("sellerUpgradeFee", config.getSellerUpgradeFee()));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @PutMapping("/order-rules/return-window-days")
    @Operation(summary = "Cập nhật số ngày cho phép trả hàng")
    public ResponseEntity<?> updateReturnWindowDays(@RequestParam Integer returnWindowDays) {
        try {
            OrderRuleConfig config = orderRuleConfigService.updateRules(null, null, returnWindowDays, null, null);
            return ok("Return window days updated successfully", Map.of("returnWindowDays", config.getReturnWindowDays()));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @PutMapping("/order-rules/bike-post-fee")
    @Operation(summary = "Cập nhật phí đăng xe")
    public ResponseEntity<?> updateBikePostFee(@RequestParam Long bikePostFee) {
        try {
            OrderRuleConfig config = orderRuleConfigService.updateRules(null, null, null, bikePostFee, null);
            return ok("Bike post fee updated successfully", Map.of("bikePostFee", config.getBikePostFee()));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @PutMapping("/order-rules/inspection-fee")
    @Operation(summary = "Cập nhật phí kiểm định")
    public ResponseEntity<?> updateInspectionFee(@RequestParam Long inspectionFee) {
        try {
            OrderRuleConfig config = orderRuleConfigService.updateRules(null, null, null, null, inspectionFee);
            return ok("Inspection fee updated successfully", Map.of("inspectionFee", config.getInspectionFee()));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/orders")
    @Operation(summary = "Danh sách tất cả giao dịch")
    public ResponseEntity<?> listOrders(@RequestParam(required = false) String status) {
        List<Order> orders;
        if (status == null || status.isBlank()) {
            orders = orderRepository.findAll();
        } else {
            try {
                Order.OrderStatus target = Order.OrderStatus.valueOf(status.trim().toUpperCase());
                orders = orderRepository.findAll().stream().filter(o -> o.getStatus() == target).toList();
            } catch (IllegalArgumentException e) {
                return badRequest("Invalid order status");
            }
        }
        List<OrderResponse> data = orders.stream().map(OrderResponse::fromEntity).toList();
        return ok("Orders retrieved successfully", data);
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Chi tiết giao dịch")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .<ResponseEntity<?>>map(order -> ok("Order retrieved successfully", OrderResponse.fromEntity(order)))
                .orElseGet(() -> notFound("Order not found"));
    }

    @PutMapping("/orders/{id}/update-status")
    @Operation(summary = "Cập nhật trạng thái khi cần can thiệp")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason) {
        Order.OrderStatus targetStatus;
        try {
            targetStatus = Order.OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return badRequest("Invalid order status");
        }
        return orderRepository.findById(id).<ResponseEntity<?>>map(order -> {
            order.setStatus(targetStatus);
            Map<String, Object> data = new HashMap<>();
            data.put("order", OrderResponse.fromEntity(orderRepository.save(order)));
            data.put("reason", reason != null ? reason : "");
            return ok("Order status updated", data);
        }).orElseGet(() -> notFound("Order not found"));
    }

    @GetMapping("/payments")
    @Operation(summary = "Danh sách thanh toán")
    public ResponseEntity<?> listPayments(@RequestParam(required = false) List<String> status) {
        return listTransactions(status);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Danh sách tất cả transactions")
    public ResponseEntity<?> listTransactions(@RequestParam(required = false) List<String> status) {
        List<PointTransaction.TransactionStatus> statuses = null;
        if (status != null && !status.isEmpty()) {
            statuses = status.stream()
                    .map(s -> { try { return PointTransaction.TransactionStatus.valueOf(s.trim().toUpperCase()); } catch (Exception e) { return null; } })
                    .filter(Objects::nonNull).toList();
        }
        List<PointTransactionDto> dtos = walletService.getAllTransactions(statuses).stream()
                .map(PointTransactionDto::from).toList();
        return ok("Transactions retrieved successfully", dtos);
    }

    @GetMapping("/fees")
    @Operation(summary = "Danh sách phí hệ thống")
    public ResponseEntity<?> listSystemFees() {
        double commissionRate = orderRuleConfigService.getCommissionRate();
        List<Order> completed = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED).toList();
        long totalCommission = 0L;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Order o : completed) {
            long amount = o.getAmountPoints() != null ? o.getAmountPoints() : 0L;
            long fee = Math.round(amount * commissionRate);
            totalCommission += fee;
            Map<String, Object> row = new HashMap<>();
            row.put("orderId", o.getId());
            row.put("amountPoints", amount);
            row.put("commissionRate", commissionRate);
            row.put("commissionFee", fee);
            rows.add(row);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "System fees retrieved successfully");
        response.put("data", rows);
        response.put("summary", Map.of("totalCompletedOrders", rows.size(), "totalCommissionFee", totalCommission));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Danh sách yêu cầu rút tiền")
    public ResponseEntity<?> getWithdrawals(@RequestParam(required = false) List<String> status) {
        List<PointTransaction.TransactionStatus> statuses = null;
        if (status != null && !status.isEmpty()) {
            statuses = status.stream()
                    .map(s -> { try { return PointTransaction.TransactionStatus.valueOf(s.trim().toUpperCase()); } catch (Exception e) { return null; } })
                    .filter(Objects::nonNull).toList();
        }
        List<PointTransactionDto> dtos = walletService.getWithdrawals(statuses).stream()
                .map(PointTransactionDto::from).toList();
        return ok("Withdrawals retrieved successfully", dtos);
    }

    @PostMapping("/withdrawals/{transactionId}/approve")
    @Operation(summary = "Duyệt yêu cầu rút tiền")
    public ResponseEntity<?> approveWithdrawal(@PathVariable Long transactionId) {
        walletService.approveWithdrawal(transactionId);
        return ok("Withdrawal approved and completed", null);
    }

    @PostMapping("/withdrawals/{transactionId}/reject")
    @Operation(summary = "Từ chối yêu cầu rút tiền")
    public ResponseEntity<?> rejectWithdrawal(
            @PathVariable Long transactionId,
            @RequestParam String reason) {
        walletService.rejectWithdrawal(transactionId, reason);
        return ok("Withdrawal rejected and points refunded", null);
    }

    @PutMapping("/transactions/{transactionId}/cancel")
    @Operation(summary = "Hủy transaction")
    public ResponseEntity<?> cancelTransaction(
            @PathVariable Long transactionId,
            @RequestParam(required = false) String reason) {
        walletService.cancelTransaction(transactionId, reason);
        return ok("Transaction cancelled", Map.of("transactionId", transactionId));
    }
}
