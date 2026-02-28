package com.bikeexchange.controller;

import com.bikeexchange.dto.request.OrderCreateRequest;
import com.bikeexchange.dto.response.OrderResponse;
import com.bikeexchange.model.Order;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.OrderService;
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

    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Approve an Order", description = "Buyer confirms receipt to release escrowed points to the seller.")
    public ResponseEntity<?> approveOrder(
            @Parameter(description = "ID of the order to approve", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.approveOrder(id, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Order completed and points released to seller");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }
}
