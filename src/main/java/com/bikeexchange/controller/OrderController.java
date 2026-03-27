package com.bikeexchange.controller;

import com.bikeexchange.dto.request.OrderCreateRequest;
import com.bikeexchange.dto.request.OrderDeliverRequest;
import com.bikeexchange.dto.request.OrderReturnRequest;
import com.bikeexchange.service.service.OrderRuleConfigService;
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
@Tag(name = "Quản Lý Đơn Hàng", description = "API mua xe đạp và quản lý quy trình mua hàng")
@SecurityRequirement(name = "Bearer Token")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRuleConfigService orderRuleConfigService;

    @GetMapping("/my-purchases")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] Xem lịch sử mua hàng", description = "Người mua xem toàn bộ lịch sử mua hàng. Hỗ trợ lọc theo 1 trạng thái (dropdown, tùy chọn), bao gồm chi tiết đánh giá, khả năng đánh giá và dòng thời gian của từng đơn hàng.")
    public ResponseEntity<?> getMyPurchases(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "status", required = false) Order.OrderStatus status) {
        List<String> statusParams = status == null ? null : List.of(status.name());
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
        response.put("data", purchases);
        response.put("summary", summary);
        return ResponseEntity.ok(response);
    }

        @GetMapping("/my-sales")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "[SELLER] Xem lịch sử bán hàng", description = "Người bán xem toàn bộ lịch sử bán hàng. Hỗ trợ lọc theo 1 trạng thái (dropdown, tùy chọn), bao gồm chi tiết đánh giá của người mua và dòng thời gian của từng đơn hàng.")
        public ResponseEntity<?> getMySales(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "status", required = false) Order.OrderStatus status) {
        List<String> statusParams = status == null ? null : List.of(status.name());
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
        response.put("data", sales);
        response.put("summary", summary);
        return ResponseEntity.ok(response);
        }

    @GetMapping("/pending-confirmations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] Danh sách đơn cần xác nhận", description = "Người bán xem danh sách các đơn đang chờ xác nhận nhận đơn (trạng thái ESCROWED).")
    public ResponseEntity<?> getPendingConfirmations(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        List<OrderResponse> orders = orderService.getSellerPendingConfirmations(currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Pending confirmation orders retrieved successfully");
        response.put("data", orders);
        response.put("summary", Map.of("totalCount", orders.size(), "status", Order.OrderStatus.ESCROWED.name()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER/SELLER] Xem dòng thời gian đơn hàng", description = "Người mua hoặc người bán xem đầy đủ dòng thời gian của một đơn hàng, bao gồm chi tiết đánh giá và khả năng đánh giá theo vai trò.")
    public ResponseEntity<?> getOrderHistory(
            @PathVariable Long id,
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
    @Operation(summary = "[BUYER] Tạo đơn hàng", description = "Người mua tạo đơn hàng cho xe đạp. Điểm sẽ được ký quỹ (đóng băng). Hệ thống tự sinh idempotencyKey nếu client không truyền. Transition: bike ACTIVE/VERIFIED -> RESERVED, order -> ESCROWED.")
    public ResponseEntity<?> createOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody OrderCreateRequest request) {
        boolean replayed = orderService.isReplayRequest(currentUser.getId(), request.getBikeId(), request.getIdempotencyKey());
        Order order = orderService.createOrder(currentUser.getId(), request.getBikeId(), request.getIdempotencyKey());
        long returnWindowTotalMinutes = orderRuleConfigService.getReturnWindowTotalMinutes();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", replayed ? "Order already created previously (idempotent)" : "Order created and points escrowed successfully");
        response.put("data", OrderResponse.fromEntity(order, returnWindowTotalMinutes));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] Hủy đơn hàng", description = "Người mua chỉ được hủy đơn khi người bán chưa accept (trạng thái ESCROWED). Điểm ký quỹ sẽ được hoàn về ví người mua. Transition: ESCROWED -> CANCELLED, bike RESERVED -> ACTIVE.")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Order order = orderService.cancelOrder(id, currentUser.getId());
        long returnWindowTotalMinutes = orderRuleConfigService.getReturnWindowTotalMinutes();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Order cancelled and points released back to buyer");
        response.put("data", OrderResponse.fromEntity(order, returnWindowTotalMinutes));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] Xác nhận nhận đơn", description = "Người bán accept đơn hàng để bắt đầu xử lý giao hàng. Transition: ESCROWED -> ACCEPTED.")
    public ResponseEntity<?> acceptOrder(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Order order = orderService.acceptOrder(id, currentUser.getId());
        long returnWindowTotalMinutes = orderRuleConfigService.getReturnWindowTotalMinutes();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Seller accepted order successfully");
        response.put("data", OrderResponse.fromEntity(order, returnWindowTotalMinutes));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/seller-cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] Hủy đơn hàng", description = "Người bán có thể hủy đơn khi đơn đang ESCROWED hoặc ACCEPTED. Điểm escrow sẽ được hoàn lại cho người mua và xe được mở bán lại. Transition: ESCROWED/ACCEPTED -> CANCELLED, bike RESERVED -> ACTIVE.")
    public ResponseEntity<?> sellerCancelOrder(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        long returnWindowTotalMinutes = orderRuleConfigService.getReturnWindowTotalMinutes();
        Order order = orderService.sellerCancelOrder(id, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Seller cancelled order successfully. Escrow refunded to buyer.");
        response.put("data", OrderResponse.fromEntity(order, returnWindowTotalMinutes));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] Đánh dấu đã gửi hàng", description = "Người bán nhập đơn vị vận chuyển, mã vận đơn và ghi chú (tuỳ chọn) để đánh dấu đã gửi cho đơn vị vận chuyển. Transition: ACCEPTED -> SHIPPED.")
    public ResponseEntity<?> markDelivered(
             @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody OrderDeliverRequest request) {

        Order order = orderService.markDelivered(
                id,
                currentUser.getId(),
                request.getShippingCarrier(),
                request.getTrackingCode(),
                request.getShippingNote());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Order marked as shipped. Please confirm delivered when the buyer receives the item.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-delivery")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] Xác nhận đã giao hàng", description = "Người bán xác nhận hàng đã giao thành công tới người mua (không tích hợp tracking bên thứ 3). Bắt đầu đếm thời gian theo cấu hình trả hàng để buyer xác nhận nhận hàng hoặc hệ thống tự giải ngân. Transition: SHIPPED -> DELIVERED.")
    public ResponseEntity<?> confirmDelivery(
             @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.confirmDelivered(id, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Order marked as delivered. Waiting for buyer to confirm receipt.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-receipt")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] Xác nhận đã nhận hàng", description = "Người mua xác nhận đã nhận hàng. Điểm được giải ngân cho người bán ngay lập tức. Nếu người mua không xác nhận, điểm sẽ tự động giải ngân theo thời gian trả hàng được cấu hình trong hệ thống. Transition: DELIVERED -> COMPLETED, bike RESERVED -> SOLD.")
    public ResponseEntity<?> confirmReceipt(
             @PathVariable Long id,
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
    @Operation(summary = "[BUYER] Yêu cầu trả hàng", description = "Chỉ áp dụng cho đơn đang DELIVERED. Người mua phải gửi yêu cầu trong thời gian trả hàng được cấu hình kể từ thời điểm giao và bắt buộc cung cấp lý do trả hàng. Điểm chỉ được hoàn khi người bán xác nhận đã nhận lại hàng (confirm-return). Nếu người bán không xác nhận, người mua có thể mở return dispute để admin xử lý. Transition: DELIVERED -> RETURN_REQUESTED.")
    public ResponseEntity<?> requestReturn(
             @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody OrderReturnRequest request) {

        Order order = orderService.requestReturn(id, currentUser.getId(), request.getReason());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Return requested. Waiting for seller to confirm they received the item back.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-return")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] Xác nhận đã nhận hàng trả", description = "Người bán xác nhận đã nhận lại hàng trả. Điểm được hoàn ngay cho người mua. Transition: RETURN_REQUESTED -> REFUNDED, bike RESERVED -> ACTIVE.")
    public ResponseEntity<?> confirmReturn(
             @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {

        Order order = orderService.confirmReturn(id, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "[SELLER ACTION] Return confirmed. Points have been refunded to buyer.");
        response.put("data", OrderResponse.fromEntity(order));
        return ResponseEntity.ok(response);
    }
}
