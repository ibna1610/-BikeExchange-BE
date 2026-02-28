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
import com.bikeexchange.service.InspectionService;
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
            @Parameter(description = "Filter by bike listing id", example = "10") @RequestParam(required = false) Long bike_id,
            @Parameter(description = "Filter by seller id", example = "3") @RequestParam(required = false) Long sellerId,
            @Parameter(description = "Filter by inspector user id", example = "2") @RequestParam(required = false) Long inspector_id,
            @Parameter(description = "Filter by inspection status", example = "REQUESTED") @RequestParam(required = false) InspectionRequest.RequestStatus status,
            @Parameter(description = "Created at from (ISO-8601)", example = "2026-02-01T00:00:00") @RequestParam(required = false) String date_from,
            @Parameter(description = "Created at to (ISO-8601)", example = "2026-02-28T23:59:59") @RequestParam(required = false) String date_to,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page", example = "20") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<InspectionRequest> spec = Specification.where(null);

        if (bike_id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("bike").get("id"), bike_id));
        }
        if (sellerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("bike").get("seller").get("id"), sellerId));
        }
        if (inspector_id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("inspector").get("id"), inspector_id));
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
            @Parameter(description = "Inspection request id", example = "5") @PathVariable Long inspectionId) {
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

    @PutMapping("/{inspectionId}")
    @PreAuthorize("hasRole('INSPECTOR')")
    @Operation(summary = "Update inspection status", description = "Roles: Inspector (ASSIGNED/IN_PROGRESS/INSPECTED). Must be an Inspector.")
    public ResponseEntity<?> updateStatus(
            @Parameter(description = "Inspection request id", example = "5") @PathVariable Long inspectionId,
            @Parameter(description = "New status", example = "ASSIGNED") @RequestParam InspectionRequest.RequestStatus status,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElse(null);
        if (inspection == null) {
            return ResponseEntity.notFound().build();
        }
        Long userId = currentUser.getId();
        if (status == InspectionRequest.RequestStatus.IN_PROGRESS) {
            inspection.setStatus(InspectionRequest.RequestStatus.IN_PROGRESS);
            inspection.setStartedAt(LocalDateTime.now());
        } else if (status == InspectionRequest.RequestStatus.INSPECTED) {
            inspection.setStatus(InspectionRequest.RequestStatus.INSPECTED);
            inspection.setCompletedAt(LocalDateTime.now());
        } else if (status == InspectionRequest.RequestStatus.ASSIGNED) {
            inspection = inspectionService.assignInspector(inspectionId, userId);
        } else {
            inspection.setStatus(status);
            inspection.setUpdatedAt(LocalDateTime.now());
        }
        inspectionRepository.save(inspection);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionResponse.fromEntity(inspection));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{inspectionId}/report")
    @PreAuthorize("hasRole('INSPECTOR')")
    @Operation(summary = "Submit inspection report", description = "Roles: Inspector. Include medias: [{url,type,sortOrder}]")
    public ResponseEntity<?> submitReport(
            @Parameter(description = "Inspection request id", example = "5") @PathVariable Long inspectionId,
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
    @Operation(summary = "Approve inspection request", description = "Roles: Admin. Approving marks bike as VERIFIED and releases commission.")
    public ResponseEntity<?> approveInspection(
            @Parameter(description = "Inspection request id", example = "5") @PathVariable Long inspectionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Long adminId = currentUser.getId();
        InspectionReport report = inspectionService.adminApproveInspection(inspectionId, adminId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionReportResponse.fromEntity(report));
        return ResponseEntity.ok(response);
    }
}
