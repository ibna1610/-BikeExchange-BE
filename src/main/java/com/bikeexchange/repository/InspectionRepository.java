package com.bikeexchange.repository;

import com.bikeexchange.model.InspectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionRepository extends JpaRepository<InspectionRequest, Long>, JpaSpecificationExecutor<InspectionRequest> {
    java.util.List<InspectionRequest> findByStatus(InspectionRequest.RequestStatus status);
    org.springframework.data.domain.Page<InspectionRequest> findByStatus(InspectionRequest.RequestStatus status, org.springframework.data.domain.Pageable pageable);
    
    java.util.List<InspectionRequest> findByInspectorIdOrderByCreatedAtDesc(Long inspectorId);
    org.springframework.data.domain.Page<InspectionRequest> findByInspectorId(Long inspectorId, org.springframework.data.domain.Pageable pageable);
    
    long countByInspectorId(Long inspectorId);
    long countByInspectorIdAndStatus(Long inspectorId, InspectionRequest.RequestStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT i FROM InspectionRequest i WHERE i.inspector.id = :inspectorId AND i.status IN :statuses ORDER BY i.createdAt DESC")
    java.util.List<InspectionRequest> findByInspectorIdAndStatusIn(Long inspectorId, java.util.Collection<InspectionRequest.RequestStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM InspectionRequest i WHERE i.status IN :statuses")
    java.util.List<InspectionRequest> findByStatusIn(@org.springframework.data.repository.query.Param("statuses") java.util.Collection<InspectionRequest.RequestStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(i.feePoints) FROM InspectionRequest i WHERE i.status IN :statuses")
    Long sumFeePointsByStatusIn(@org.springframework.data.repository.query.Param("statuses") java.util.Collection<InspectionRequest.RequestStatus> statuses);
}
