package com.bikeexchange.service;

import com.bikeexchange.model.Review;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.ReviewRepository;
import com.bikeexchange.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReviewService reviewService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void createReview_updatesSellerRating() {
        User reviewer = new User(); reviewer.setId(1L);
        User seller = new User(); seller.setId(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(reviewRepository.save(org.mockito.Mockito.any(Review.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findBySellerIdOrderByCreatedAtDesc(2L))
                .thenReturn(Collections.singletonList(new Review()));

        Review r = reviewService.createReview(1L, 2L, null, 4, "Good seller");
        assertEquals(4, r.getRating());
    }
}
