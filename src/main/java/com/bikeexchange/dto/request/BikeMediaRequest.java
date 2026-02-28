package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Payload for submitting a media object (Image or Video) linked to a bike")
public class BikeMediaRequest {
    @Schema(description = "URL of the image or video", example = "https://example.com/image.jpg")
    private String url;

    @Schema(description = "Type of media. Options: IMAGE, VIDEO", example = "IMAGE")
    private String type;

    @Schema(description = "Ordering index for the media logic display", example = "1")
    private Integer sortOrder;
}
