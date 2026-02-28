package com.bikeexchange.repository;

import com.bikeexchange.model.InspectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionRepository extends JpaRepository<InspectionRequest, Long>, JpaSpecificationExecutor<InspectionRequest> {
}
