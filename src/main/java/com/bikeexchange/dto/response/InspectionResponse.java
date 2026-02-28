package com.bikeexchange.dto.response;

import com.bikeexchange.model.InspectionRequest;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InspectionResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private Long ownerId;
    private String ownerName;
    private Long inspectorId;
    private String inspectorName;
    private InspectionRequest.RequestStatus status;
    private Long feePoints;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public static InspectionResponse fromEntity(InspectionRequest request) {
        InspectionResponse res = new InspectionResponse();
        res.setId(request.getId());
        if (request.getListing() != null) {
            res.setListingId(request.getListing().getId());
            res.setListingTitle(request.getListing().getTitle());
            if (request.getListing().getSeller() != null) {
                res.setOwnerId(request.getListing().getSeller().getId());
                res.setOwnerName(request.getListing().getSeller().getFullName());
            }
        }
        if (request.getInspector() != null) {
            res.setInspectorId(request.getInspector().getId());
            res.setInspectorName(request.getInspector().getFullName());
        }
        res.setStatus(request.getStatus());
        res.setFeePoints(request.getFeePoints());
        res.setCreatedAt(request.getCreatedAt());
        res.setStartedAt(request.getStartedAt());
        res.setCompletedAt(request.getCompletedAt());
        return res;
    }
}
