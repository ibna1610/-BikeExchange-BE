package com.bikeexchange.repository;

import com.bikeexchange.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);
    Page<Report> findByReportType(Report.ReportType type, Pageable pageable);
    long countByStatus(Report.ReportStatus status);
}
