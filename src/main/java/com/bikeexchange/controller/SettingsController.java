package com.bikeexchange.controller;

import com.bikeexchange.model.OrderRuleConfig;
import com.bikeexchange.repository.OrderRuleConfigRepository;
import com.bikeexchange.service.service.OrderRuleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/public/settings")
@Tag(name = "Public Settings", description = "Public APIs to view system configurations like fees and rates.")
public class SettingsController {

    @Autowired
    private OrderRuleConfigRepository orderRuleConfigRepository;

    @Autowired
    private OrderRuleConfigService orderRuleConfigService;

    private OrderRuleConfig getConfig() {
        return orderRuleConfigRepository.findById(OrderRuleConfig.SINGLETON_ID)
                .orElseThrow(() -> new RuntimeException("System configuration not found"));
    }

    @GetMapping("/bike-post-fee")
    @Operation(summary = "Xem phí đăng xe")
    public ResponseEntity<?> getBikePostFee() {
        return ResponseEntity.ok(Map.of("success", true, "data", getConfig().getBikePostFee()));
    }

    @GetMapping("/seller-upgrade-fee")
    @Operation(summary = "Xem phí nâng cấp lên seller")
    public ResponseEntity<?> getSellerUpgradeFee() {
        return ResponseEntity.ok(Map.of("success", true, "data", getConfig().getSellerUpgradeFee()));
    }

    @GetMapping("/inspection-fee")
    @Operation(summary = "Xem phí kiểm định")
    public ResponseEntity<?> getInspectionFee() {
        return ResponseEntity.ok(Map.of("success", true, "data", getConfig().getInspectionFee()));
    }

    @GetMapping("/return-window-days")
    @Operation(summary = "Xem số ngày cho phép trả hàng")
    public ResponseEntity<?> getReturnWindowDays() {
        return ResponseEntity.ok(Map.of("success", true, "data", getConfig().getReturnWindowDays()));
    }

    @GetMapping("/commission-rate")
    @Operation(summary = "Xem tỷ lệ hoa hồng")
    public ResponseEntity<?> getCommissionRate() {
        return ResponseEntity.ok(Map.of("success", true, "data", orderRuleConfigService.getCommissionRatePercent()));
    }
}
