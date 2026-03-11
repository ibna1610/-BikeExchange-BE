package com.bikeexchange.service.service;

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
    private OrderRepository orderRepository;

    @Transactional
    public Review createReview(Long reviewerId, Long orderId, Integer rating, String comment) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getBuyer().getId().equals(reviewerId)) {
            throw new IllegalArgumentException("Only the buyer can review this order");
        }
        
        if (order.getStatus() != Order.OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only review completed orders");
        }
        
        // Check if review already exists for this order
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new IllegalArgumentException("Review already exists for this order");
        }
        
        User reviewer = order.getBuyer();
        User seller = order.getBike().getSeller();

        Review r = new Review();
        r.setReviewer(reviewer);
        r.setSeller(seller);
        r.setOrder(order);
        r.setRating(rating);
        r.setComment(comment);
        Review saved = reviewRepository.save(r);

        // Recalculate seller rating (simple average)
        var reviews = reviewRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId());
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
        userService.updateUserRating(seller.getId(), avg);

        return saved;
    }

    public List<Review> listBySeller(Long sellerId) {
        return reviewRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }
}
