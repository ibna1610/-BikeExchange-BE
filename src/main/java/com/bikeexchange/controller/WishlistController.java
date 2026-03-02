package com.bikeexchange.controller;

import com.bikeexchange.model.Bike;
import com.bikeexchange.model.User;
import com.bikeexchange.model.Wishlist;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.WishlistRepository;
import com.bikeexchange.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Wishlist> items = wishlistRepository.findByBuyerId(currentUser.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", items));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> add(@AuthenticationPrincipal UserPrincipal currentUser, @RequestParam Long bikeId) {
        Bike bike = bikeRepository.findById(bikeId).orElse(null);
        if (bike == null) return ResponseEntity.notFound().build();
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (wishlistRepository.findByBuyerIdAndBikeId(user.getId(), bikeId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Already in wishlist"));
        }

        Wishlist w = new Wishlist();
        w.setBuyer(user);
        w.setBike(bike);
        wishlistRepository.save(w);
        return ResponseEntity.ok(Map.of("success", true, "message", "Added to wishlist", "data", w));
    }

    @DeleteMapping("/{bikeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> remove(@AuthenticationPrincipal UserPrincipal currentUser, @PathVariable Long bikeId) {
        var opt = wishlistRepository.findByBuyerIdAndBikeId(currentUser.getId(), bikeId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        wishlistRepository.delete(opt.get());
        return ResponseEntity.ok(Map.of("success", true, "message", "Removed from wishlist"));
    }
}
