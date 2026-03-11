package com.bikeexchange.controller;

import com.bikeexchange.model.Bike;
import com.bikeexchange.model.User;
import com.bikeexchange.model.Wishlist;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.WishlistRepository;
import com.bikeexchange.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/buyer/wishlist")
@Tag(name = "Buyer - Wishlist", description = "Buyer wishlist APIs")
@SecurityRequirement(name = "Bearer Token")
public class WishlistController {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "GET /api/buyer/wishlist", description = "Xem danh sach xe yeu thich cua buyer hien tai")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Wishlist> items = wishlistRepository.findByBuyerId(currentUser.getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Wishlist retrieved successfully", "data", items));
    }

    @PostMapping("/{bikeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "POST /api/buyer/wishlist/{bikeId}", description = "Them xe vao wishlist")
    public ResponseEntity<?> add(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long bikeId) {

        Bike bike = bikeRepository.findById(bikeId).orElse(null);
        if (bike == null) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Bike not found"));
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null) return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));

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
    @Operation(summary = "DELETE /api/buyer/wishlist/{bikeId}", description = "Xoa xe khoi wishlist")
    public ResponseEntity<?> remove(@AuthenticationPrincipal UserPrincipal currentUser, @PathVariable Long bikeId) {
        var opt = wishlistRepository.findByBuyerIdAndBikeId(currentUser.getId(), bikeId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Wishlist item not found"));
        wishlistRepository.delete(opt.get());
        return ResponseEntity.ok(Map.of("success", true, "message", "Removed from wishlist"));
    }
}
