package com.bikeexchange.controller;

import com.bikeexchange.model.Report;
import com.bikeexchange.repository.UserReportRepository;
import com.bikeexchange.service.UserReportService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Reports", description = "6.5 Quản lý báo cáo vi phạm và khiếu nại")
@SecurityRequirement(name = "Bearer Token")
public class AdminReportController extends AdminBaseController {

    @Autowired private UserReportRepository userReportRepository;
    @Autowired private UserReportService userReportService;

    @GetMapping("/reports")
    @Operation(summary = "Danh sách báo cáo vi phạm")
    public ResponseEntity<?> listReports(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean pending,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        if (pending != null && pending) {
            return ok("Reports retrieved successfully", userReportService.listPendingReports(pageable));
        }
        if (type != null && !type.isBlank()) {
            try {
                Report.ReportType t = Report.ReportType.valueOf(type.toUpperCase());
                return ok("Reports retrieved successfully", userReportService.listByType(t, pageable));
            } catch (IllegalArgumentException e) {
                return badRequest("Invalid type");
            }
        }
        return ok("Reports retrieved successfully", userReportService.listPendingReports(pageable));
    }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "Chi tiết báo cáo")
    public ResponseEntity<?> getReportById(@PathVariable Long reportId) {
        return userReportRepository.findById(reportId)
                .<ResponseEntity<?>>map(r -> ok("Report retrieved successfully", r))
                .orElseGet(() -> notFound("Report not found"));
    }

    @PutMapping("/reports/{reportId}/process")
    @Operation(summary = "Xử lý báo cáo (chuyển sang REVIEWING)")
    public ResponseEntity<?> processReport(
            @PathVariable Long reportId,
            @RequestParam(required = false) String adminNote) {
        try {
            Report updated = userReportService.resolveReport(reportId, Report.ReportStatus.REVIEWING, adminNote);
            return ok("Report is now being processed", updated);
        } catch (IllegalArgumentException e) {
            return notFound("Report not found");
        }
    }

    @PutMapping("/reports/{reportId}/resolve")
    @Operation(summary = "Đóng khiếu nại (RESOLVED / REJECTED)")
    public ResponseEntity<?> resolveReport(
            @PathVariable Long reportId,
            @Parameter(description = "Trang thai dong khieu nai", schema = @Schema(implementation = Report.ReportStatus.class))
            @RequestParam(required = false) Report.ReportStatus resolution,
            @RequestParam(required = false) String adminNote) {
        try {
            Report.ReportStatus status = resolution != null ? resolution : Report.ReportStatus.RESOLVED;
            if (status != Report.ReportStatus.RESOLVED && status != Report.ReportStatus.REJECTED) {
                return badRequest("resolution must be RESOLVED or REJECTED");
            }
            Report updated = userReportService.resolveReport(reportId, status, adminNote);
            return ok("Report resolved successfully", updated);
        } catch (IllegalArgumentException e) {
            return badRequest("Invalid resolution status");
        }
    }
}
