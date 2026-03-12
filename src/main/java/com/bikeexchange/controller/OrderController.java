package com.bikeexchange.controller;

import com.bikeexchange.dto.request.OrderCreateRequest;
import com.bikeexchange.dto.response.BuyerPurchaseHistoryResponse;
import com.bikeexchange.dto.response.OrderHistoryDetailResponse;
import com.bikeexchange.dto.response.OrderResponse;
import com.bikeexchange.dto.response.SellerSalesHistoryResponse;
import com.bikeexchange.model.Order;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
@Tag(name = "Order Management", description = "APIs for buying bikes and managing purchase workflows")
@SecurityRequirement(name = "Bearer Token")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/my-purchases")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] View Purchase History", description = "Buyer views a complete purchase history. Supports optional status filtering and includes review detail, reviewability, and order timeline for each order.")
    public ResponseEntity<?> getMyPurchases(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(example = "COMPLETED") @RequestParam(name = "status", required = false) List<String> statusParams) {

        List<BuyerPurchaseHistoryResponse> purchases = orderService.getBuyerPurchaseHistory(currentUser.getId(), statusParams);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", purchases.size());
        summary.put("reviewableCount", purchases.stream().filter(BuyerPurchaseHistoryResponse::isCanReview).count());
        summary.put("reviewedCount", purchases.stream().filter(BuyerPurchaseHistoryResponse::isReviewed).count());
        Map<String, Long> byStatus = purchases.stream()
            .collect(Collectors.groupingBy(
                        purchase -> purchase.getOrder().getStatus().name(),
                LinkedHashMap::new,
                Collectors.counting()));
        summary.put("byStatus", byStatus);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Purchase history retrieved successfully");
        response.put("data", purchases);
        response.put("summary", summary);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-sales")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] View Sales History", description = "Seller views a complete sales history. Supports optional status filtering and includes buyer review detail and order timeline for each order.")
    public ResponseEntity<?> getMySales(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(example = "COMPLETED") @RequestParam(name = "status", required = false) List<String> statusParams) {

        List<SellerSalesHistoryResponse> sales = orderService.getSellerSalesHistory(currentUser.getId(), statusParams);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", sales.size());
        summary.put("reviewedCount", sales.stream().filter(SellerSalesHistoryResponse::isReviewed).count());
        Map<String, Long> byStatus = sales.stream()
                .collect(Collectors.groupingBy(
                        sale -> sale.getOrder().getStatus().name(),
                        LinkedHashMap::new,
                        Collectors.counting()));
        summary.put("byStatus", byStatus);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sales history retrieved successfully");
        response.put("data", sales);
        response.put("summary", summary);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER/SELLER] View Order Timeline", description = "Buyer or seller views the full timeline of one order, including review detail and role-aware reviewability.")
    public ResponseEntity<?> getOrderHistory(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        OrderHistoryDetailResponse historyDetail = orderService.getOrderHistoryDetail(id, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "[BUYER/SELLER] Order history retrieved successfully");
        response.put("data", historyDetail);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] Create an Order", description = "Buyer creates an order for a bike. Points will be escrowed (frozen).")
    public ResponseEntity<?> createOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody OrderCreateRequest request) {

        Order order = orderService.createOrder(currentUser.getId(), request.getBikeId(), request.getIdempotencyKey());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Order created and points escrowed successfully");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] Cancel an Order", description = "Buyer cancels the order before delivery, releasing escrowed points back to buyer.")
    public ResponseEntity<?> cancelOrder(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.cancelOrder(id, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Order cancelled and points released back to buyer");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] Mark Order as Delivered", description = "Seller marks the order as delivered. Buyer has 7 days from delivery to request a return.")
    public ResponseEntity<?> markDelivered(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.markDelivered(id, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Order marked as delivered. Waiting for buyer to confirm receipt.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-receipt")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] Confirm Receipt", description = "Buyer confirms receipt. Points are released to seller immediately. If buyer does not confirm, points auto-release to seller after 7 days from delivery.")
    public ResponseEntity<?> confirmReceipt(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.confirmReceipt(id, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "[BUYER ACTION] Receipt confirmed. Points have been released to seller.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/request-return")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] Request Return", description = "Buyer requests a return within 7 days of delivery. Points are refunded after seller confirms the returned item is received.")
    public ResponseEntity<?> requestReturn(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.requestReturn(id, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Return requested. Waiting for seller to confirm they received the item back.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-return")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] Confirm Return Received", description = "Seller confirms the returned item is received. Points are immediately refunded to buyer.")
    public ResponseEntity<?> confirmReturn(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.confirmReturn(id, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "[SELLER ACTION] Return confirmed. Points have been refunded to buyer.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }
}
