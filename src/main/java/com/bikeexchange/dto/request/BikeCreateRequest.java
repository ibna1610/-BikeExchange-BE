package com.bikeexchange.dto.request;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Data
@Schema(description = "Request payload for creating or updating a bike listing")
public class BikeCreateRequest {

    @Schema(description = "Title of the bike listing", example = "Giant Escape 3 2022")
    private String title;

    @Schema(description = "Detailed description of the bike's condition and features", example = "Bán xe đạp Giant Escape 3 đời 2022 màu xanh lục, còn mới 95%. Xe chính chủ, bảo dưỡng định kỳ.")
    private String description;

    @Schema(description = "Brand of the bike", example = "Giant")
    private String brand;

    @Schema(description = "Specific model of the bike", example = "Escape 3")
    private String model;

    @Schema(description = "Year of manufacture", example = "2022")
    private Integer year;

    @Schema(description = "Price equivalent in points", example = "45000")
    private Long pricePoints;

    @Schema(description = "Condition of the bike (e.g. New, Like New, Good, Fair)", example = "Like New")
    private String condition;

    @Schema(description = "Category/type of the bike (e.g. Road, Mountain, Hybrid)", example = "Hybrid")
    private String bikeType;

    @Schema(description = "List of images and videos attached to this bike", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<BikeMediaRequest> media;

    @Schema(description = "Category IDs associated with this bike", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<Long> categoryIds;
}
