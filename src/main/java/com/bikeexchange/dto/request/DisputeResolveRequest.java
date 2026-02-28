package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class DisputeResolveRequest {
    private String resolutionType; // REFUND or RELEASE
    private String resolutionNote;
}
