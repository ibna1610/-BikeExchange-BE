package com.bikeexchange.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InspectorDashboardResponse {
    private long totalAssigned;
    private long pendingInspections; // ASSIGNED but not started
    private long inProgressInspections; // IN_PROGRESS
    private long completedInspections; // INSPECTED, APPROVED, REJECTED
    
    private Map<String, Long> statusCounts;
    
    // List of modern tasks
    private List<InspectionResponse> recentTasks;
    private List<InspectionResponse> ongoingTasks;
}
