package com.bikeexchange.service.service;

import com.bikeexchange.model.Review;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public Review createReview(Long reviewerId, Long sellerId, Integer rating, String comment) {
        // Validate reviewer exists
        User reviewer = userService.getUserById(reviewerId)
            .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));
        
        // Validate seller exists
        User seller = userService.getUserById(sellerId)
            .orElseThrow(() -> new IllegalArgumentException("Seller not found"));
        
        // Check if reviewer already reviewed this seller
        if (reviewRepository.existsByReviewerIdAndSellerId(reviewerId, sellerId)) {
            throw new IllegalArgumentException("You have already reviewed this seller");
        }
        
        Review r = new Review();
        r.setReviewer(reviewer);
        r.setSeller(seller);
        r.setOrder(null);
        r.setRating(rating);
        r.setComment(comment);
        Review saved = reviewRepository.save(r);

        // Recalculate seller rating (simple average)
        var reviews = reviewRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        userService.updateUserRating(sellerId, avg);

        return saved;
    }

    public List<Review> listBySeller(Long sellerId) {
        return reviewRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }
}
