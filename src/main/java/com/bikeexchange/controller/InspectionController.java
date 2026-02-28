package com.bikeexchange.controller;

import com.bikeexchange.dto.request.InspectionReportDto;
import com.bikeexchange.dto.request.InspectionRequestDto;
import com.bikeexchange.model.InspectionReport;
import com.bikeexchange.model.InspectionRequest;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.InspectionService;
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
@RequestMapping("/inspection")
@Tag(name = "Inspection Management", description = "APIs for requesting, assigning, and managing bike inspections")
@SecurityRequirement(name = "Bearer Token")
public class InspectionController {

    @Autowired
    private InspectionService inspectionService;

    @PostMapping("/request")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> requestInspection(@AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody InspectionRequestDto request) {
        InspectionRequest inspection = inspectionService.requestInspection(currentUser.getId(), request.getListingId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Inspection requested successfully");
        response.put("data", inspection);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/assign/{inspectorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignInspector(@PathVariable Long id, @PathVariable Long inspectorId) {
        InspectionRequest inspection = inspectionService.assignInspector(id, inspectorId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Inspector assigned successfully");
        response.put("data", inspection);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("hasRole('INSPECTOR')")
    public ResponseEntity<?> submitReport(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody InspectionReportDto request) {
        InspectionReport report = inspectionService.submitReport(id, currentUser.getId(), request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Report submitted successfully");
        response.put("data", report);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/report/{reportId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveReport(@PathVariable Long reportId) {
        InspectionReport report = inspectionService.adminApproveReport(reportId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Report approved and listing verified");
        response.put("data", report);

        return ResponseEntity.ok(response);
    }
}
