package com.bikeexchange.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResponse {
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("public_id")
    private String publicId;
    
    @JsonProperty("file_size")
    private Long fileSize;
    
    @JsonProperty("uploaded_at")
    private String uploadedAt;
    
    @JsonProperty("seller_address")
    private String sellerAddress;
    
    @JsonProperty("seller_name")
    private String sellerName;
    
    @JsonProperty("cloudinary_folder")
    private String cloudinaryFolder;
    
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
}
