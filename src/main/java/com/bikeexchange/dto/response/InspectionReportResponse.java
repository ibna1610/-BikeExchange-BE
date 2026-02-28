package com.bikeexchange.dto.response;

import com.bikeexchange.model.InspectionReport;
import com.bikeexchange.model.InspectionRequest;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class InspectionReportResponse {
    private Long id;
    private Long requestId;
    private String frameCondition;
    private String groupsetCondition;
    private String wheelCondition;
    private Integer overallScore;
    private InspectionRequest.RequestStatus adminDecision;
    private String comments;
    private LocalDateTime createdAt;
    private List<MediaResponse> medias;

    @Data
    public static class MediaResponse {
        private String url;
        private String type;
        private Integer sortOrder;
    }

    public static InspectionReportResponse fromEntity(InspectionReport report) {
        InspectionReportResponse res = new InspectionReportResponse();
        res.setId(report.getId());
        if (report.getRequest() != null) {
            res.setRequestId(report.getRequest().getId());
        }
        res.setFrameCondition(report.getFrameCondition());
        res.setGroupsetCondition(report.getGroupsetCondition());
        res.setWheelCondition(report.getWheelCondition());
        res.setOverallScore(report.getOverallScore());
        res.setAdminDecision(report.getAdminDecision());
        res.setComments(report.getComments());
        res.setCreatedAt(report.getCreatedAt());

        if (report.getMedias() != null) {
            res.setMedias(report.getMedias().stream().map(m -> {
                MediaResponse mr = new MediaResponse();
                mr.setUrl(m.getUrl());
                mr.setType(m.getType().name());
                mr.setSortOrder(m.getSortOrder());
                return mr;
            }).collect(Collectors.toList()));
        }
        return res;
    }
}
