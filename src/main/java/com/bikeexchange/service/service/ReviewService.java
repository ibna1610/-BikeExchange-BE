package com.bikeexchange.service.service;

import com.bikeexchange.model.Review;
import com.bikeexchange.model.User;
import com.bikeexchange.model.Order;
import com.bikeexchange.repository.OrderRepository;
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

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public Review createReview(Long reviewerId, Long orderId, Integer rating, String comment) {
        // Validate reviewer exists
        User reviewer = userService.getUserById(reviewerId)
            .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getBuyer() == null || !order.getBuyer().getId().equals(reviewerId)) {
            throw new IllegalArgumentException("Only the buyer of this order can review");
        }

        if (order.getStatus() != Order.OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Only COMPLETED orders can be reviewed");
        }

        if (reviewRepository.existsByOrderId(orderId)) {
            throw new IllegalArgumentException("This order has already been reviewed");
        }

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
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        userService.updateUserRating(seller.getId(), avg);

        return saved;
    }

    public List<Review> listBySeller(Long sellerId) {
        return reviewRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }
}
