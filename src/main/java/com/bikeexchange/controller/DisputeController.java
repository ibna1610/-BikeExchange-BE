package com.bikeexchange.controller;

import com.bikeexchange.dto.response.DisputeSummaryResponse;
import com.bikeexchange.dto.request.DisputeResolutionType;
import com.bikeexchange.dto.request.DisputeResolveRequest;
import com.bikeexchange.dto.request.ReturnDisputeRequest;
import com.bikeexchange.model.Dispute;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.service.DisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Dispute Management", description = "APIs for creating and resolving disputes")
@SecurityRequirement(name = "Bearer Token")
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    @GetMapping("/orders/my-disputes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] Xem danh sách tranh chấp của tôi", description = "Người mua xem toàn bộ tranh chấp liên quan đến các đơn hàng của mình.")
    public ResponseEntity<?> getMyDisputes(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        List<Dispute> disputes = disputeService.getBuyerDisputes(currentUser.getId());
        List<DisputeSummaryResponse> disputeSummaries = disputes.stream()
            .map(DisputeSummaryResponse::fromEntity)
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fetched buyer disputes successfully");
        response.put("data", disputeSummaries);
        response.put("summary", Map.of("totalCount", disputeSummaries.size()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/disputes/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Lấy tất cả tranh chấp cần xử lý", description = "Trả về toàn bộ tranh chấp có trạng thái OPEN hoặc INVESTIGATING để admin xử lý.")
    public ResponseEntity<?> getPendingDisputes() {
        List<Dispute> disputes = disputeService.getPendingDisputes();
        List<DisputeSummaryResponse> disputeSummaries = disputes.stream()
                .map(DisputeSummaryResponse::fromEntity)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fetched pending disputes successfully");
        response.put("data", disputeSummaries);
        response.put("summary", Map.of("totalCount", disputeSummaries.size()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/disputes/resolved")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Lấy tất cả tranh chấp đã xử lý", description = "Trả về toàn bộ tranh chấp đã xử lý với trạng thái RESOLVED_REFUND hoặc RESOLVED_RELEASE.")
    public ResponseEntity<?> getResolvedDisputes() {
        List<Dispute> disputes = disputeService.getResolvedDisputes();
        List<DisputeSummaryResponse> disputeSummaries = disputes.stream()
                .map(DisputeSummaryResponse::fromEntity)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fetched resolved disputes successfully");
        response.put("data", disputeSummaries);
        response.put("summary", Map.of("totalCount", disputeSummaries.size()));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders/{orderId}/return-dispute")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[BUYER] Mở tranh chấp hoàn hàng", description = "Người mua mở tranh chấp khi seller từ chối hoàn hàng trả, cung cấp lý do và thông tin liên hệ để admin xử lý.")
    public ResponseEntity<?> createReturnDispute(
             @PathVariable Long orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody ReturnDisputeRequest request) {
        Dispute dispute = disputeService.createReturnDispute(orderId, currentUser.getId(), request);
        DisputeSummaryResponse disputeSummary = DisputeSummaryResponse.fromEntity(dispute);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Return dispute opened successfully. Admin will review and make a decision.");
        response.put("data", disputeSummary);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/dispute/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Xử lý tranh chấp", description = "Admin xử lý tranh chấp bằng query param resolutionType (dropdown: REFUND hoặc RELEASE). Request body chỉ chứa resolutionNote.")
    public ResponseEntity<?> resolveDispute(@PathVariable Long id,
            @RequestParam DisputeResolutionType resolutionType,
            @RequestBody(required = false) DisputeResolveRequest request) {
        String resolutionNote = request == null ? null : request.getResolutionNote();
        Dispute dispute = disputeService.resolveDispute(id, resolutionType, resolutionNote);
        DisputeSummaryResponse disputeSummary = DisputeSummaryResponse.fromEntity(dispute);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dispute resolved successfully as " + resolutionType);
        response.put("data", disputeSummary);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/dispute/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Từ chối tranh chấp", description = "Admin từ chối tranh chấp (không đủ bằng chứng/không hợp lệ). Bắt buộc truyền resolutionNote.")
    public ResponseEntity<?> rejectDispute(@PathVariable Long id,
            @RequestBody DisputeResolveRequest request) {
        String resolutionNote = request == null ? null : request.getResolutionNote();
        Dispute dispute = disputeService.rejectDispute(id, resolutionNote);
        DisputeSummaryResponse disputeSummary = DisputeSummaryResponse.fromEntity(dispute);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dispute rejected successfully");
        response.put("data", disputeSummary);

        return ResponseEntity.ok(response);
    }

    // [SELLER] Xem danh sách tranh chấp liên quan đến xe của tôi
    @GetMapping("/orders/my-seller-disputes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[SELLER] Xem danh sách tranh chấp liên quan đến xe của tôi")
    public ResponseEntity<?> getMySellerDisputes(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        List<Dispute> disputes = disputeService.getSellerDisputes(currentUser.getId());
        List<DisputeSummaryResponse> disputeSummaries = disputes.stream()
            .map(DisputeSummaryResponse::fromEntity)
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fetched seller disputes successfully");
        response.put("data", disputeSummaries);
        response.put("summary", Map.of("totalCount", disputeSummaries.size()));

        return ResponseEntity.ok(response);
    }
}
