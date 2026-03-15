package com.bikeexchange.dto.response;

import com.bikeexchange.model.Dispute;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DisputeSummaryResponse {
    private Long id;
    private Long orderId;
    private String orderStatus;
    private Long amountPoints;
    private String bikeTitle;
    private String sellerName;
    private String sellerPhone;
    private String sellerShopName;
    private String reporterName;
    private String reporterEmail;
    private String reporterPhone;
    private String reporterAddress;
    private String reason;
    private String status;
    private String disputeType;
    private String buyerContactAddress;
    private String buyerContactPhone;
    private String buyerContactEmail;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static DisputeSummaryResponse fromEntity(Dispute dispute) {
        DisputeSummaryResponse response = new DisputeSummaryResponse();
        response.setId(dispute.getId());
        response.setOrderId(dispute.getOrder().getId());
        response.setOrderStatus(dispute.getOrder().getStatus().name());
        response.setAmountPoints(dispute.getOrder().getAmountPoints());
        response.setBikeTitle(dispute.getOrder().getBike().getTitle());
        response.setSellerName(dispute.getOrder().getBike().getSeller().getFullName());
        response.setSellerPhone(dispute.getOrder().getBike().getSeller().getPhone());
        response.setSellerShopName(dispute.getOrder().getBike().getSeller().getShopName());
        response.setReporterName(dispute.getReporter().getFullName());
        response.setReporterEmail(dispute.getReporter().getEmail());
        response.setReporterPhone(dispute.getReporter().getPhone());
        response.setReporterAddress(dispute.getReporter().getAddress());
        response.setReason(dispute.getReason());
        response.setStatus(dispute.getStatus().name());
        response.setDisputeType(dispute.getDisputeType().name());
        response.setBuyerContactAddress(dispute.getBuyerContactAddress());
        response.setBuyerContactPhone(dispute.getBuyerContactPhone());
        response.setBuyerContactEmail(dispute.getBuyerContactEmail());
        response.setResolutionNote(dispute.getResolutionNote());
        response.setCreatedAt(dispute.getCreatedAt());
        response.setResolvedAt(dispute.getResolvedAt());
        return response;
    }
}