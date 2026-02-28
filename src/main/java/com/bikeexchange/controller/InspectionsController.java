package com.bikeexchange.controller;

import com.bikeexchange.dto.request.InspectionReportDto;
import com.bikeexchange.dto.request.InspectionRequestDto;
import com.bikeexchange.model.InspectionReport;
import com.bikeexchange.model.InspectionRequest;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.repository.InspectionRepository;
import com.bikeexchange.service.InspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Inspection Management Controller
 * - List inspections with filters (bike, inspector, status, date range)
 * - Create inspection requests (seller or testing with sellerId)
 * - Update inspection status (assign/in-progress/inspected/approved/rejected)
 * - Submit inspection report with images
 * - Approve report (sets bike VERIFIED)
 */
@RestController
@RequestMapping("/inspections")
@Tag(name = "Inspection Management", description = "APIs for requesting, assigning, and managing bike inspections")
public class InspectionsController {

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private InspectionService inspectionService;

    @GetMapping
    @Operation(summary = "List inspections", description = "Roles: Public (test mode). Filters: bike_id, inspector_id, status, date_from, date_to. Returns a paginated list.")
    public ResponseEntity<?> list(
            @Parameter(description = "Filter by bike listing id", example = "10") @RequestParam(required = false) Long bike_id,
            @Parameter(description = "Filter by inspector user id", example = "2") @RequestParam(required = false) Long inspector_id,
            @Parameter(description = "Filter by inspection status", example = "REQUESTED") @RequestParam(required = false) InspectionRequest.RequestStatus status,
            @Parameter(description = "Created at from (ISO-8601)", example = "2026-02-01T00:00:00") @RequestParam(required = false) String date_from,
            @Parameter(description = "Created at to (ISO-8601)", example = "2026-02-28T23:59:59") @RequestParam(required = false) String date_to,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page", example = "20") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<InspectionRequest> spec = Specification.where(null);

        if (bike_id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("listing").get("id"), bike_id));
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
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Request inspection", description = "Roles: Seller (or supply sellerId in test mode). Body: InspectionRequestDto { listingId }")
    public ResponseEntity<?> create(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody InspectionRequestDto request,
            @Parameter(description = "Seller id when unauthenticated", example = "1") @RequestParam(name = "sellerId", required = false) Long sellerIdParam) {
        Long requesterId = currentUser != null ? currentUser.getId() : sellerIdParam;
        if (requesterId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "sellerId is required when not logged in"));
        }
        InspectionRequest inspection = inspectionService.requestInspection(requesterId, request.getListingId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", inspection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inspection", description = "Roles: Public (test mode). Returns inspection by id.")
    public ResponseEntity<?> getOne(@Parameter(description = "Inspection id", example = "5") @PathVariable Long id) {
        return inspectionRepository.findById(id)
                .map(i -> ResponseEntity.ok(Map.of("success", true, "data", i)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inspection status", description = "Roles: Inspector (ASSIGNED/IN_PROGRESS/INSPECTED), Admin (APPROVED/REJECTED). Test mode allows inspectorId param when unauthenticated.")
    public ResponseEntity<?> updateStatus(
            @Parameter(description = "Inspection id", example = "5") @PathVariable Long id,
            @Parameter(description = "New status", example = "ASSIGNED") @RequestParam InspectionRequest.RequestStatus status,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Inspector id when unauthenticated", example = "2") @RequestParam(name = "inspectorId", required = false) Long inspectorIdParam) {
        InspectionRequest inspection = inspectionRepository.findById(id)
                .orElse(null);
        if (inspection == null) {
            return ResponseEntity.notFound().build();
        }
        if (status == InspectionRequest.RequestStatus.IN_PROGRESS) {
            inspection.setStatus(InspectionRequest.RequestStatus.IN_PROGRESS);
            inspection.setStartedAt(LocalDateTime.now());
        } else if (status == InspectionRequest.RequestStatus.INSPECTED) {
            inspection.setStatus(InspectionRequest.RequestStatus.INSPECTED);
            inspection.setCompletedAt(LocalDateTime.now());
        } else if (status == InspectionRequest.RequestStatus.ASSIGNED) {
            Long inspectorId = currentUser != null ? currentUser.getId() : inspectorIdParam;
            if (inspectorId == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "inspectorId is required when not logged in"));
            }
            inspection = inspectionService.assignInspector(id, inspectorId);
        } else {
            inspection.setStatus(status);
            inspection.setUpdatedAt(LocalDateTime.now());
        }
        inspectionRepository.save(inspection);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", inspection);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Submit inspection report", description = "Roles: Inspector. Include medias: [{url,type,sortOrder}]")
    public ResponseEntity<?> submitReport(
            @Parameter(description = "Inspection id", example = "5") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Inspector id when unauthenticated", example = "2") @RequestParam(name = "inspectorId", required = false) Long inspectorIdParam,
            @RequestBody InspectionReportDto request) {
        Long inspectorId = currentUser != null ? currentUser.getId() : inspectorIdParam;
        if (inspectorId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "inspectorId is required when not logged in"));
        }
        InspectionReport report = inspectionService.submitReport(id, inspectorId, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", report);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/report/{reportId}/approve")
    @Operation(summary = "Approve inspection report", description = "Roles: Admin. Approving marks bike as VERIFIED and releases commission.")
    public ResponseEntity<?> approveReport(@Parameter(description = "Report id", example = "7") @PathVariable Long reportId) {
        InspectionReport report = inspectionService.adminApproveReport(reportId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", report);
        return ResponseEntity.ok(response);
    }
}
