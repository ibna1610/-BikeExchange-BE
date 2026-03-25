package com.bikeexchange.dto.request;

import lombok.Data;

@Data
public class ListingComboRequest {
    private String name;
    private Long pointsCost;
    private Integer postLimit;
    private boolean active = true;
}
