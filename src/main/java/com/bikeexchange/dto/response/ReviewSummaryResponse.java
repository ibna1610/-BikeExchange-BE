package com.bikeexchange.dto.response;

import com.bikeexchange.model.Review;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewSummaryResponse {
    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewSummaryResponse fromEntity(Review review) {
        ReviewSummaryResponse response = new ReviewSummaryResponse();
        response.setId(review.getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
