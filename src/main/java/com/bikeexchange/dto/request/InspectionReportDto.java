package com.bikeexchange.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class InspectionReportDto {
    private String frameCondition;
    private String groupsetCondition;
    private String wheelCondition;
    private Integer overallScore;
    private String comments;
    private List<ReportMediaRequest> medias;
}
