package com.bikeexchange.dto.response;

import com.bikeexchange.model.Bike;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class BikeResponse {
    private Long id;
    private String title;
    private String description;
    private String brand;
    private String model;
    private Integer year;
    private Long pricePoints;
    private Integer mileage;
    private String condition;
    private String bikeType;
    private String location;
    private Bike.BikeStatus status;
    private Bike.InspectionStatus inspectionStatus;
    private Long sellerId;
    private Integer views;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MediaResponse> media;

    @Data
    public static class MediaResponse {
        private String url;
        private String type;
        private Integer sortOrder;
    }

    public static BikeResponse fromEntity(Bike bike) {
        BikeResponse res = new BikeResponse();
        res.setId(bike.getId());
        res.setTitle(bike.getTitle());
        res.setDescription(bike.getDescription());
        res.setBrand(bike.getBrand().getName());
        res.setModel(bike.getModel());
        res.setYear(bike.getYear());
        res.setPricePoints(bike.getPricePoints());
        res.setMileage(bike.getMileage());
        res.setCondition(bike.getCondition());
        res.setBikeType(bike.getBikeType());
        res.setLocation(bike.getLocation());
        res.setStatus(bike.getStatus());
        res.setInspectionStatus(bike.getInspectionStatus());
        if (bike.getSeller() != null) {
            res.setSellerId(bike.getSeller().getId());
        }
        res.setViews(bike.getViews());
        res.setCreatedAt(bike.getCreatedAt());
        res.setUpdatedAt(bike.getUpdatedAt());

        if (bike.getMedia() != null) {
            res.setMedia(bike.getMedia().stream().map(m -> {
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
