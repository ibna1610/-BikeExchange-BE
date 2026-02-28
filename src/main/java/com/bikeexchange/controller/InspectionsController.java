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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @GetMapping
    @Operation(summary = "List inspections", description = "Roles: Public (test mode). Filters: bike_id, inspector_id, status, date_from, date_to. Returns a paginated list.")
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
            spec = spec.and((root, query, cb) -> cb.equal(root.get("listing").get("id"), bike_id));
        }
        if (sellerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("listing").get("seller").get("id"), sellerId));
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
    @Operation(summary = "Request inspection", description = "Roles: Seller (or supply sellerId in test mode). Body: InspectionRequestDto { bikeId }")
    public ResponseEntity<?> create(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody InspectionRequestDto request,
            @Parameter(description = "Seller id when unauthenticated", example = "1") @RequestParam(name = "sellerId", required = false) Long sellerIdParam) {
        Long requesterId = currentUser != null ? currentUser.getId() : sellerIdParam;
        if (requesterId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "sellerId is required when not logged in"));
        }
        InspectionRequest inspection = inspectionService.requestInspection(requesterId, request.getBikeId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionResponse.fromEntity(inspection));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{inspectionId}")
    @Operation(summary = "Get inspection", description = "Roles: Public (test mode). Returns inspection by id.")
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
    @Operation(summary = "Update inspection status", description = "Roles: Inspector (ASSIGNED/IN_PROGRESS/INSPECTED), Admin (APPROVED/REJECTED). Test mode allows inspectorId param when unauthenticated.")
    public ResponseEntity<?> updateStatus(
            @Parameter(description = "Inspection request id", example = "5") @PathVariable Long inspectionId,
            @Parameter(description = "New status", example = "ASSIGNED") @RequestParam InspectionRequest.RequestStatus status,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Inspector id when unauthenticated", example = "2") @RequestParam(name = "inspectorId", required = false) Long inspectorIdParam) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
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
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "inspectorId is required when not logged in"));
            }
            inspection = inspectionService.assignInspector(inspectionId, inspectorId);
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
    @Operation(summary = "Submit inspection report", description = "Roles: Inspector. Include medias: [{url,type,sortOrder}]")
    public ResponseEntity<?> submitReport(
            @Parameter(description = "Inspection request id", example = "5") @PathVariable Long inspectionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Inspector user id when unauthenticated", example = "2") @RequestParam(name = "inspectorId", required = false) Long inspectorIdParam,
            @RequestBody InspectionReportDto request) {
        Long inspectorId = currentUser != null ? currentUser.getId() : inspectorIdParam;
        if (inspectorId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "inspectorId is required when not logged in"));
        }
        InspectionReport report = inspectionService.submitReport(inspectionId, inspectorId, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionReportResponse.fromEntity(report));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{inspectionId}/approve")
    @Operation(summary = "Approve inspection request", description = "Roles: Admin. Approving marks bike as VERIFIED and releases commission. This approves the overall inspection request based on the submitted report.")
    public ResponseEntity<?> approveInspection(
            @Parameter(description = "Inspection request id", example = "5") @PathVariable Long inspectionId) {
        InspectionReport report = inspectionService.adminApproveInspection(inspectionId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", InspectionReportResponse.fromEntity(report));
        return ResponseEntity.ok(response);
    }
}
