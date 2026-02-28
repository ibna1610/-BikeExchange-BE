package com.bikeexchange.repository;

import com.bikeexchange.model.InspectionReportMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionReportMediaRepository extends JpaRepository<InspectionReportMedia, Long> {
}
