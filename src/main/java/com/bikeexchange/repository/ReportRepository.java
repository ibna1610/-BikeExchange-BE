package com.bikeexchange.repository;

import com.bikeexchange.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(Report.ReportStatus status);
    List<Report> findByReportedUserId(Long reportedUserId);
    List<Report> findByBikeId(Long bikeId);
    List<Report> findByReporterId(Long reporterId);
    List<Report> findByReportType(Report.ReportType reportType);
    Long countByStatus(Report.ReportStatus status);
}
