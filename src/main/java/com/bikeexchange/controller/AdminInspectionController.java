package com.bikeexchange.controller;

import com.bikeexchange.model.InspectionReport;
import com.bikeexchange.model.InspectionRequest;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.InspectionRepository;
import com.bikeexchange.repository.ReportRepository;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Inspections", description = "6.6 Quản lý kiểm định")
@SecurityRequirement(name = "Bearer Token")
public class AdminInspectionController extends AdminBaseController {

    @Autowired private InspectionRepository inspectionRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private InspectionService inspectionService;

    @GetMapping("/inspection-requests")
    @Operation(summary = "Danh sách yêu cầu kiểm định")
    public ResponseEntity<?> listInspectionRequests(
            @Parameter(description = "Filter theo trang thai request", schema = @Schema(implementation = InspectionRequest.RequestStatus.class))
            @RequestParam(required = false) InspectionRequest.RequestStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        if (status == null) {
            return ok("Inspection requests retrieved successfully", inspectionRepository.findAll(pageable));
        }
        return ok("Inspection requests retrieved successfully", inspectionRepository.findByStatus(status, pageable));
    }

    @GetMapping("/inspection-reports")
    @Operation(summary = "Danh sách báo cáo kiểm định")
    public ResponseEntity<?> listInspectionReports(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return ok("Inspection reports retrieved successfully", reportRepository.findAll(pageable));
    }

    @GetMapping("/inspectors")
    @Operation(summary = "Danh sách inspector")
    public ResponseEntity<?> listInspectors(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return ok("Inspectors retrieved successfully",
                userRepository.findByRole(User.UserRole.INSPECTOR, pageable));
    }

    @PutMapping("/inspectors/{id}/approve")
    @Operation(summary = "Duyệt inspector")
    public ResponseEntity<?> approveInspector(@PathVariable Long id) {
        return userRepository.findById(id).<ResponseEntity<?>>map(user -> {
            if (user.getRole() != User.UserRole.INSPECTOR) return badRequest("User is not an inspector");
            user.setStatus("ACTIVE");
            user.setIsVerified(true);
            return ok("Inspector approved", userRepository.save(user));
        }).orElseGet(() -> notFound("Inspector not found"));
    }

    @PutMapping("/inspectors/{id}/suspend")
    @Operation(summary = "Tạm ngưng inspector")
    public ResponseEntity<?> suspendInspector(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        String reasonValue = (reason == null || reason.isBlank()) ? "Suspended by admin" : reason;
        return userRepository.findById(id).<ResponseEntity<?>>map(user -> {
            if (user.getRole() != User.UserRole.INSPECTOR) return badRequest("User is not an inspector");
            user.setStatus("SUSPENDED");
            userRepository.save(user);
            return ok("Inspector suspended", Map.of("inspectorId", id, "reason", reasonValue));
        }).orElseGet(() -> notFound("Inspector not found"));
    }

    @GetMapping("/inspections/pending")
    @Operation(summary = "Danh sách kiểm định chờ xử lý")
    public ResponseEntity<?> listPendingInspections(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return ok("Pending inspections retrieved successfully",
                inspectionRepository.findByStatus(InspectionRequest.RequestStatus.INSPECTED, pageable));
    }

    @PutMapping("/inspections/{inspectionId}/reject")
    @Operation(summary = "Từ chối kết quả kiểm định")
    public ResponseEntity<?> rejectInspection(
            @PathVariable Long inspectionId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InspectionReport report = inspectionService.adminRejectInspection(inspectionId, currentUser.getId(), reason);
        return ok("Inspection rejected", report);
    }
}
