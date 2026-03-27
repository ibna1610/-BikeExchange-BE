package com.bikeexchange.controller;

import com.bikeexchange.dto.response.SystemWalletSummaryResponse;
import com.bikeexchange.service.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/wallet")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Wallet", description = "Admin APIs for viewing system wallet and platform balance")
@SecurityRequirement(name = "Bearer Token")
public class AdminWalletController extends AdminBaseController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/system-summary")
    @Operation(summary = "Xem tổng quan ví hệ thống (tiền đóng băng, đơn hàng đang escrow)")
    public ResponseEntity<?> getSystemWalletSummary() {
        SystemWalletSummaryResponse summary = adminService.getSystemWalletSummary();
        return ok("System wallet summary retrieved successfully", summary);
    }

    @GetMapping("/revenue-summary")
    @Operation(summary = "Xem tổng doanh thu hệ thống (tiền người dùng đã tiêu vào web)")
    public ResponseEntity<?> getSystemRevenueSummary() {
        return ok("System revenue summary retrieved successfully", adminService.getSystemRevenueSummary());
    }
}
