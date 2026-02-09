package com.bikeexchange.repository;

import com.bikeexchange.model.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByBikeId(Long bikeId);
    List<Inspection> findByInspectorId(Long inspectorId);
    List<Inspection> findByRequesterId(Long requesterId);
    List<Inspection> findByStatus(Inspection.InspectionStatus status);
    Optional<Inspection> findFirstByBikeIdAndStatusOrderByCreatedAtDesc(Long bikeId, Inspection.InspectionStatus status);
}
