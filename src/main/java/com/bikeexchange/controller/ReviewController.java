package com.bikeexchange.controller;

import com.bikeexchange.model.Review;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createReview(@AuthenticationPrincipal UserPrincipal currentUser,
                                          @RequestParam Long sellerId,
                                          @RequestParam Integer rating,
                                          @RequestParam(required = false) String comment) {
        Review r = reviewService.createReview(currentUser.getId(), sellerId, null, rating, comment);
        return ResponseEntity.ok(Map.of("success", true, "data", r));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> listBySeller(@PathVariable Long sellerId) {
        return ResponseEntity.ok(Map.of("success", true, "data", reviewService.listBySeller(sellerId)));
    }
}
