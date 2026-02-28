package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class ReportMediaRequest {
    private String url;
    private String type; // IMAGE or VIDEO
    private Integer sortOrder;
}
