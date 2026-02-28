package com.bikeexchange.controller;

import com.bikeexchange.dto.request.DepositRequest;
import com.bikeexchange.dto.request.WithdrawRequest;
import com.bikeexchange.dto.response.PointTransactionDto;
import com.bikeexchange.model.PointTransaction;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/wallet")
@Tag(name = "Wallet & Points", description = "APIs for managing user wallet, deposits, and withdrawals")
@SecurityRequirement(name = "Bearer Token")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping
    public ResponseEntity<?> getWallet(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "userId", required = false) Long userIdParam) {
        Long userId = currentUser != null ? currentUser.getId() : userIdParam;
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "userId is required when not logged in"));
        }
        UserWallet wallet = walletService.getWallet(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", wallet);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "userId", required = false) Long userIdParam,
            @Parameter(description = "Filter by transaction types: DEPOSIT, WITHDRAW, SPEND, EARN, ESCROW_HOLD, ESCROW_RELEASE, COMMISSION", example = "DEPOSIT")
            @RequestParam(name = "type", required = false) java.util.List<String> typeParams) {
        Long userId = currentUser != null ? currentUser.getId() : userIdParam;
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "userId is required when not logged in"));
        }
        List<PointTransaction> transactions = walletService.getTransactions(userId, typeParams);
        java.util.List<PointTransactionDto> dtos = transactions.stream().map(PointTransactionDto::from).toList();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", transactions.size());
        long totalAmount = transactions.stream().mapToLong(t -> t.getAmount() != null ? t.getAmount() : 0L).sum();
        summary.put("totalAmount", totalAmount);
        Map<String, Map<String, Long>> byType = new HashMap<>();
        for (com.bikeexchange.model.PointTransaction tx : transactions) {
            String type = tx.getType() != null ? tx.getType().name() : "UNKNOWN";
            Map<String, Long> agg = byType.computeIfAbsent(type, k -> new HashMap<>());
            long count = agg.getOrDefault("count", 0L) + 1;
            long amount = agg.getOrDefault("amount", 0L) + (tx.getAmount() != null ? tx.getAmount() : 0L);
            agg.put("count", count);
            agg.put("amount", amount);
        }
        summary.put("byType", byType);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", dtos);
        response.put("summary", summary);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/deposit")
    public ResponseEntity<?> depositPoints(@AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody DepositRequest request) {
        UserWallet wallet = walletService.depositPoints(currentUser.getId(), request.getAmount(),
                request.getReferenceId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Deposit successful");
        response.put("data", wallet);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/withdraw-request")
    public ResponseEntity<?> requestWithdraw(@AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody WithdrawRequest request) {
        UserWallet wallet = walletService.requestWithdraw(currentUser.getId(), request.getAmount(),
                request.getBankAccountConfig());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Withdraw request submitted. Pending admin approval.");
        response.put("data", wallet);

        return ResponseEntity.ok(response);
    }
}
