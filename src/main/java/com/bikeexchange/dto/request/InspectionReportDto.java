package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class InspectionReportDto {
    private String frameCondition;
    private String groupsetCondition;
    private String wheelCondition;
    private Integer overallScore;
    private String comments;
}
