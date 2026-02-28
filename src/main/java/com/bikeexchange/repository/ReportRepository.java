package com.bikeexchange.repository;

import com.bikeexchange.model.InspectionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<InspectionReport, Long> {
    Optional<InspectionReport> findByRequestId(Long requestId);
}
