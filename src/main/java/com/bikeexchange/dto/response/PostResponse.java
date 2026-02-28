package com.bikeexchange.dto.response;

import com.bikeexchange.model.Post;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResponse {
    private Long id;
    private Long sellerId;
    private Long bikeId;
    private String caption;
    private String listingType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BikeResponse bike;

    public static PostResponse fromEntity(Post post) {
        PostResponse res = new PostResponse();
        res.setId(post.getId());
        if (post.getSeller() != null) {
            res.setSellerId(post.getSeller().getId());
        }
        if (post.getBike() != null) {
            res.setBikeId(post.getBike().getId());
            res.setBike(BikeResponse.fromEntity(post.getBike()));
        }
        res.setCaption(post.getCaption());
        if (post.getListingType() != null) {
            res.setListingType(post.getListingType().name());
        }
        if (post.getStatus() != null) {
            res.setStatus(post.getStatus().name());
        }
        res.setCreatedAt(post.getCreatedAt());
        res.setUpdatedAt(post.getUpdatedAt());
        return res;
    }
}
