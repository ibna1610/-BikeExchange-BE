package com.bikeexchange.service;

import com.bikeexchange.model.Report;
import com.bikeexchange.repository.UserReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserReportService {

    @Autowired
    private UserReportRepository reportRepository;

    public Page<Report> listPendingReports(Pageable pageable) {
        return reportRepository.findByStatus(Report.ReportStatus.PENDING, pageable);
    }

    public Page<Report> listByType(Report.ReportType type, Pageable pageable) {
        return reportRepository.findByReportType(type, pageable);
    }

    @Transactional
    public Report resolveReport(Long id, Report.ReportStatus resolution, String adminNote) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        report.setStatus(resolution);
        report.setAdminNote(adminNote);
        report.setResolvedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    public long countByStatus(Report.ReportStatus status) {
        return reportRepository.countByStatus(status);
    }
}
