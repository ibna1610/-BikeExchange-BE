package com.bikeexchange.controller;

import com.bikeexchange.dto.request.DepositRequest;
import com.bikeexchange.dto.request.WithdrawRequest;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/wallet")
@Tag(name = "Wallet & Points", description = "APIs for managing user wallet, deposits, and withdrawals")
@SecurityRequirement(name = "Bearer Token")
@PreAuthorize("isAuthenticated()")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping
    public ResponseEntity<?> getWallet(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserWallet wallet = walletService.getWallet(currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", wallet);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", walletService.getTransactions(currentUser.getId()));

        return ResponseEntity.ok(response);
    }

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
