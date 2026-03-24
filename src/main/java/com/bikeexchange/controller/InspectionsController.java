package com.bikeexchange.controller;

import com.bikeexchange.dto.request.InspectionReportDto;
import com.bikeexchange.dto.request.InspectionRequestDto;
import com.bikeexchange.dto.response.HistoryResponse;
import com.bikeexchange.dto.response.InspectionReportResponse;
import com.bikeexchange.dto.response.InspectionResponse;
import com.bikeexchange.model.InspectionReport;
import com.bikeexchange.model.InspectionRequest;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.repository.HistoryRepository;
import com.bikeexchange.repository.InspectionRepository;
import com.bikeexchange.repository.ReportRepository;
import com.bikeexchange.service.service.InspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Inspection Management Controller
 * - List inspections with filters (bike, inspector, status, date range)
 * - Create inspection requests (authenticated seller)
 * - Update inspection status (assign/in-progress/inspected/approved/rejected)
 * - Submit inspection report with images
 * - Approve report (sets bike VERIFIED)
 */
@RestController
@RequestMapping("/inspections")
@Tag(name = "Inspection Management", description = "APIs for requesting, assigning, and managing bike inspections")
@SecurityRequirement(name = "Bearer Token")
public class InspectionsController {

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @GetMapping
    @Operation(summary = "List inspections", description = "Filters: bike_id, inspector_id, status, date_from, date_to. Returns a paginated list.")
    public ResponseEntity<?> list(
             @RequestParam(name = "sellerId", required = false) Long sellerId,
            @Parameter(schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {
                    "REQUESTED", "ASSIGNED", "INSPECTED", "APPROVED",
                    "REJECTED" })) @RequestParam(name = "status", required = false) InspectionRequest.RequestStatus status,
             @RequestParam(name = "date_from", required = false) String date_from,
             @RequestParam(name = "date_to", required = false) String date_to,
             @RequestParam(name = "page", defaultValue = "0") int page,
             @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<InspectionRequest> spec = Specification.where(null);

        if (sellerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("bike").get("seller").get("id"), sellerId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (date_from != null && !date_from.isBlank()) {
            LocalDateTime from = LocalDateTime.parse(date_from);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (date_to != null && !date_to.isBlank()) {
            LocalDateTime to = LocalDateTime.parse(date_to);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        Page<InspectionRequest> result = inspectionRepository.findAll(spec, pageable);
        Page<InspectionResponse> dtoPage = result.map(InspectionResponse::fromEntity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", dtoPage);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request inspection", description = "Roles: Seller (authenticated). Body includes bikeId and optional scheduling info (preferredDate, preferredTimeSlot, address, contactPhone, notes).")
    public ResponseEntity<?> create(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody InspectionRequestDto request) {
        Long requesterId = currentUser.getId();
        InspectionRequest inspection = inspectionService.requestInspection(requesterId, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionResponse.fromEntity(inspection));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{inspectionId}")
    @Operation(summary = "Get inspection", description = "Returns inspection by id with report and history.")
    public ResponseEntity<?> getOne(
             @PathVariable(name = "inspectionId") Long inspectionId) {
        return inspectionRepository.findById(inspectionId)
                .map(i -> {
                    InspectionResponse inspection = InspectionResponse.fromEntity(i);
                    InspectionReportResponse report = reportRepository.findByRequestId(inspectionId)
                            .map(InspectionReportResponse::fromEntity)
                            .orElse(null);
                    List<HistoryResponse> history = historyRepository
                            .findByEntityTypeAndEntityIdOrderByTimestampAsc("inspection", inspectionId)
                            .stream().map(HistoryResponse::fromEntity).toList();
                    Map<String, Object> data = new HashMap<>();
                    data.put("inspection", inspection);
                    data.put("report", report);
                    data.put("history", history);
                    return ResponseEntity.ok(Map.of("success", true, "data", data));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/bikes/{bikeId}/report")
    @Operation(summary = "Get inspection report by bike ID", description = "Returns the latest inspection report for a specific bike.")
    public ResponseEntity<?> getReportByBikeId(
             @PathVariable(name = "bikeId") Long bikeId) {
        InspectionReport report = inspectionService.getReportByBikeId(bikeId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionReportResponse.fromEntity(report));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{inspectionId}")
    @PreAuthorize("hasRole('INSPECTOR')")
    @Operation(summary = "Update inspection status", description = "Roles: Inspector (ASSIGNED/REJECTED). Must be an Inspector.")
    public ResponseEntity<?> updateStatus(
             @PathVariable("inspectionId") Long inspectionId,
            @Parameter(schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {
                    "REQUESTED", "ASSIGNED",
                    "REJECTED" })) @RequestParam("status") InspectionRequest.RequestStatus status,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser.getId();
        InspectionRequest inspection = inspectionService.updateInspectionStatus(inspectionId, status, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionResponse.fromEntity(inspection));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{inspectionId}/reject")
    @PreAuthorize("hasRole('INSPECTOR')")
    @Operation(summary = "Reject inspection request (Inspector)", description = "Roles: Inspector. Cancels the request and refunds the seller.")
    public ResponseEntity<?> rejectByInspector(
            @PathVariable("inspectionId") Long inspectionId,
            @RequestBody com.bikeexchange.dto.request.InspectionRejectRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long inspectorId = currentUser.getId();
        InspectionRequest inspection = inspectionService.inspectorRejectRequest(inspectionId, request.getReason(), inspectorId);
        return ResponseEntity.ok(Map.of("success", true, "data", InspectionResponse.fromEntity(inspection)));
    }

    @PostMapping("/{inspectionId}/report")
    @PreAuthorize("hasRole('INSPECTOR')")
    @Operation(summary = "Submit inspection report", description = "Roles: Inspector. Include medias: [{url,type,sortOrder}]")
    public ResponseEntity<?> submitReport(
             @PathVariable("inspectionId") Long inspectionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody InspectionReportDto request) {
        Long inspectorId = currentUser.getId();
        InspectionReport report = inspectionService.submitReport(inspectionId, inspectorId, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionReportResponse.fromEntity(report));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{inspectionId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve inspection request", description = "Roles: Admin. logic: result=true marks bike VERIFIED, result=false marks REJECTED (inspection fail). Both pay inspector.")
    public ResponseEntity<?> approveInspection(
             @PathVariable("inspectionId") Long inspectionId,
             @RequestParam(name = "result", defaultValue = "true") boolean isPassed,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Long adminId = currentUser.getId();
        InspectionReport report = inspectionService.adminApproveInspection(inspectionId, adminId, isPassed);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionReportResponse.fromEntity(report));
        return ResponseEntity.ok(response);
    }
}
