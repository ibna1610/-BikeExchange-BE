package com.bikeexchange.repository;

import com.bikeexchange.model.InspectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionRepository extends JpaRepository<InspectionRequest, Long>, JpaSpecificationExecutor<InspectionRequest> {
    java.util.List<InspectionRequest> findByStatus(InspectionRequest.RequestStatus status);
    org.springframework.data.domain.Page<InspectionRequest> findByStatus(InspectionRequest.RequestStatus status, org.springframework.data.domain.Pageable pageable);
}
