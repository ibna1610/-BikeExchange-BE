package com.bikeexchange.controller;

import com.bikeexchange.dto.request.OrderCreateRequest;
import com.bikeexchange.dto.response.OrderResponse;
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
import java.util.Map;

@RestController
@RequestMapping("/orders")
@Tag(name = "Order Management", description = "APIs for buying bikes and managing purchase workflows")
@SecurityRequirement(name = "Bearer Token")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create an Order", description = "Buyer creates an order for a bike. Points will be escrowed (frozen).")
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
    @Operation(summary = "Cancel an Order", description = "Buyer cancels the order before delivery, releasing escrowed points back to buyer.")
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
    @Operation(summary = "Mark Order as Delivered", description = "Seller marks the order as delivered. Buyer then has 7 days after confirming receipt to request a return.")
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
    @Operation(summary = "Confirm Receipt", description = "Buyer confirms they received the item. Points are released to seller immediately. If buyer does not confirm, points auto-release to seller after 7 days from delivery.")
    public ResponseEntity<?> confirmReceipt(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.confirmReceipt(id, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Receipt confirmed. Points have been released to seller.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/request-return")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request Return", description = "Buyer requests to return the item within 7 days of delivery. Points refunded after seller confirms return.")
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
    @Operation(summary = "Confirm Return Received", description = "Seller confirms they received the returned item. Points are immediately refunded to buyer.")
    public ResponseEntity<?> confirmReturn(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.confirmReturn(id, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Return confirmed. Points have been refunded to buyer.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }
}
