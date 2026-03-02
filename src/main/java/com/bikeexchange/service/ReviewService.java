package com.bikeexchange.service;

import com.bikeexchange.model.Review;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.ReviewRepository;
import com.bikeexchange.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public Review createReview(Long reviewerId, Long sellerId, Long orderId, Integer rating, String comment) {
        User reviewer = userRepository.findById(reviewerId).orElseThrow(() -> new RuntimeException("Reviewer not found"));
        User seller = userRepository.findById(sellerId).orElseThrow(() -> new RuntimeException("Seller not found"));

        Review r = new Review();
        r.setReviewer(reviewer);
        r.setSeller(seller);
        r.setOrder(null);
        r.setRating(rating);
        r.setComment(comment);
        Review saved = reviewRepository.save(r);

        // Recalculate seller rating (simple average)
        var reviews = reviewRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
        userService.updateUserRating(sellerId, avg);

        return saved;
    }

    public List<Review> listBySeller(Long sellerId) {
        return reviewRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }
}
