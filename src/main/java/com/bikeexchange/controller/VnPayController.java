package com.bikeexchange.controller;

import com.bikeexchange.config.VNPAYConfig;
import com.bikeexchange.service.VnPayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.bikeexchange.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/vnpay")
@SecurityRequirement(name = "Bearer Token")
public class VnPayController {

    @Autowired
    private VnPayService vnPayService;
    @Autowired
    private VNPAYConfig vnpayConfig;

    @GetMapping("/create-payment")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create VNPay Payment URL", description = "Generates a URL to redirect user to VNPay for wallet deposit.")
    public ResponseEntity<?> createPayment(
            @Parameter(description = "Amount in VND to deposit", example = "50000") @RequestParam("amount") Long amount,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request) {
        Long userId = currentUser.getId();
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số tiền không hợp lệ"));
        }
        String ipAddr = request.getRemoteAddr();
        String paymentUrl = vnPayService.generatePaymentUrl(amount, userId, ipAddr);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("paymentUrl", paymentUrl);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/vnpay-payment-return")
    public ResponseEntity<?> paymentReturn(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> flat = new HashMap<>();
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            flat.put(e.getKey(), e.getValue()[0]);
        }
        if (!vnPayService.verifySignature(flat)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Chữ ký không hợp lệ"));
        }
        String tmn = flat.getOrDefault("vnp_TmnCode", "");
        if (!vnpayConfig.getTmnCode().equals(tmn)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Sai TmnCode"));
        }
        String rspCode = flat.getOrDefault("vnp_ResponseCode", "99");
        String txStatus = flat.getOrDefault("vnp_TransactionStatus", "99");
        if ("00".equals(rspCode) && "00".equals(txStatus)) {
            String txnRef = flat.getOrDefault("vnp_TxnRef", "");
            String orderInfo = flat.getOrDefault("vnp_OrderInfo", "");
            Long userId = extractUserId(txnRef, orderInfo);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Thiếu thông tin người dùng"));
            }
            Long amountVnd = Long.parseLong(flat.getOrDefault("vnp_Amount", "0")) / 100;
            String referenceId = flat.getOrDefault("vnp_TransactionNo", "");
            if (referenceId == null || referenceId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu vnp_TransactionNo"));
            }
            vnPayService.depositIfNotProcessed(userId, amountVnd, referenceId);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Thanh toán thành công, điểm đã được cộng");
            return ResponseEntity.ok(res);
        } else {
            return ResponseEntity.ok(Map.of("success", false, "message", "Thanh toán thất bại", "code", rspCode));
        }
    }

    @GetMapping("/ipn")
    public ResponseEntity<String> ipn(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> flat = new HashMap<>();
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            flat.put(e.getKey(), e.getValue()[0]);
        }
        if (!vnPayService.verifySignature(flat)) {
            return ResponseEntity.ok("RspCode=97&Message=Invalid signature");
        }
        String tmn = flat.getOrDefault("vnp_TmnCode", "");
        if (!vnpayConfig.getTmnCode().equals(tmn)) {
            return ResponseEntity.ok("RspCode=03&Message=Invalid TmnCode");
        }
        String rspCode = flat.getOrDefault("vnp_ResponseCode", "99");
        String txStatus = flat.getOrDefault("vnp_TransactionStatus", "99");
        if ("00".equals(rspCode) && "00".equals(txStatus)) {
            String txnRef = flat.getOrDefault("vnp_TxnRef", "");
            String orderInfo = flat.getOrDefault("vnp_OrderInfo", "");
            Long userId = extractUserId(txnRef, orderInfo);
            if (userId == null) {
                return ResponseEntity.ok("RspCode=02&Message=Order not found");
            }
            Long amountVnd = Long.parseLong(flat.getOrDefault("vnp_Amount", "0")) / 100;
            String referenceId = flat.getOrDefault("vnp_TransactionNo", "");
            if (referenceId == null || referenceId.isBlank()) {
                return ResponseEntity.ok("RspCode=06&Message=Missing TransactionNo");
            }
            vnPayService.depositIfNotProcessed(userId, amountVnd, referenceId);
            return ResponseEntity.ok("RspCode=00&Message=Confirm Success");
        }
        return ResponseEntity.ok("RspCode=" + rspCode + "&Message=Payment failed");
    }

    private Long extractUserId(String txnRef, String orderInfo) {
        try {
            if (txnRef != null && txnRef.startsWith("WALLET-")) {
                String[] parts = txnRef.split("-");
                return Long.parseLong(parts[1]);
            }
            if (orderInfo != null && orderInfo.startsWith("WALLET_DEPOSIT_")) {
                String s = orderInfo.replace("WALLET_DEPOSIT_", "");
                return Long.parseLong(s);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
