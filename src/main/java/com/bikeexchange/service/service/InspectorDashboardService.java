package com.bikeexchange.service.service;

import com.bikeexchange.dto.response.InspectionResponse;
import com.bikeexchange.dto.response.InspectorDashboardResponse;
import com.bikeexchange.model.InspectionRequest;
import com.bikeexchange.repository.InspectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InspectorDashboardService {

    @Autowired
    private InspectionRepository inspectionRepository;

    @Transactional(readOnly = true)
    public InspectorDashboardResponse getDashboardData(Long inspectorId) {
        long totalAssigned = inspectionRepository.countByInspectorId(inspectorId);
        long pending = inspectionRepository.countByInspectorIdAndStatus(inspectorId, InspectionRequest.RequestStatus.ASSIGNED);
        long inProgress = inspectionRepository.countByInspectorIdAndStatus(inspectorId, InspectionRequest.RequestStatus.IN_PROGRESS);
        long completed = inspectionRepository.countByInspectorIdAndStatus(inspectorId, InspectionRequest.RequestStatus.INSPECTED)
                        + inspectionRepository.countByInspectorIdAndStatus(inspectorId, InspectionRequest.RequestStatus.APPROVED)
                        + inspectionRepository.countByInspectorIdAndStatus(inspectorId, InspectionRequest.RequestStatus.REJECTED);

        // Status counts for chart
        Map<String, Long> statusCounts = Arrays.stream(InspectionRequest.RequestStatus.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        status -> inspectionRepository.countByInspectorIdAndStatus(inspectorId, status)
                ));

        // Ongoing tasks (ASSIGNED or IN_PROGRESS)
        List<InspectionResponse> ongoingTasks = inspectionRepository.findByInspectorIdAndStatusIn(
                inspectorId, 
                Arrays.asList(InspectionRequest.RequestStatus.ASSIGNED, InspectionRequest.RequestStatus.IN_PROGRESS)
        ).stream().map(InspectionResponse::fromEntity).collect(Collectors.toList());

        // Recent tasks (top 5)
        List<InspectionResponse> recentTasks = inspectionRepository.findByInspectorIdOrderByCreatedAtDesc(inspectorId)
                .stream()
                .limit(5)
                .map(InspectionResponse::fromEntity)
                .collect(Collectors.toList());

        return InspectorDashboardResponse.builder()
                .totalAssigned(totalAssigned)
                .pendingInspections(pending)
                .inProgressInspections(inProgress)
                .completedInspections(completed)
                .statusCounts(statusCounts)
                .ongoingTasks(ongoingTasks)
                .recentTasks(recentTasks)
                .build();
    }
}
