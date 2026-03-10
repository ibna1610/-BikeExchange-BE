package com.bikeexchange.controller;

import com.bikeexchange.dto.request.BikeCreateRequest;
import com.bikeexchange.model.Bike;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.service.BikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/bikes")
@Tag(name = "Bike Listing Management", description = "APIs for managing bike listings, updating status, and searching (formerly listings)")
@SecurityRequirement(name = "Bearer Token")
public class BikeController {

    @Autowired
    private BikeService bikeService;

    @GetMapping
    @Operation(summary = "Search and Filter Bikes", description = "Retrieve a paginated list of bikes. Filters: keyword, category_id, status (repeat param or comma-separated).")
    public ResponseEntity<?> getListings(
            @Parameter(example = "Giant") @RequestParam(name = "keyword", required = false) String keyword,
            @Parameter(example = "1") @RequestParam(name = "category_id", required = false) Long categoryId,
            @Parameter(example = "VERIFIED") @RequestParam(name = "status", required = false) String status,
            @Parameter(example = "1000") @RequestParam(name = "price_min", required = false) Long priceMin,
            @Parameter(example = "5000") @RequestParam(name = "price_max", required = false) Long priceMax,
            @Parameter(example = "2018") @RequestParam(name = "min_year", required = false) Integer minYear,
            @Parameter(example = "54cm") @RequestParam(name = "frame_size", required = false) String frameSize,
            @Parameter(example = "true") @RequestParam(name = "sort_by_rating", defaultValue = "false") boolean sortByRating,
            @Parameter(example = "0") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(example = "20") @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bike> result = bikeService.searchBikesAdvanced(keyword, categoryId, status, priceMin, priceMax,
                minYear, frameSize, sortByRating, pageable);

        Page<com.bikeexchange.dto.response.BikeResponse> dtoPage = result
                .map(com.bikeexchange.dto.response.BikeResponse::fromEntity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", dtoPage);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Bike Details", description = "Fetch complete details of a single bike listing by its ID")
    public ResponseEntity<?> getListing(
            @Parameter(example = "1") @PathVariable(name = "id") Long id) {
        Bike bike = bikeService.getBikeById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", com.bikeexchange.dto.response.BikeResponse.fromEntity(bike));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasRole('SELLER')")
    @Operation(summary = "Create a New Bike", description = "Create a new bike. Must be authenticated.")
    public ResponseEntity<?> createBike(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody BikeCreateRequest request) {
        Long sellerId = currentUser.getId();
        Bike bike = bikeService.createBike(sellerId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bike created successfully in ACTIVE state");
        response.put("data", com.bikeexchange.dto.response.BikeResponse.fromEntity(bike));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an Existing Bike", description = "Modify details of an existing bike. Must be the owner.")
    public ResponseEntity<?> updateBike(
            @Parameter(example = "1") @PathVariable(name = "id") Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody BikeCreateRequest request) {
        Bike bike = bikeService.updateBike(id, currentUser.getId(), request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bike updated successfully");
        response.put("data", com.bikeexchange.dto.response.BikeResponse.fromEntity(bike));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete / Archive a Bike", description = "Soft delete or mark a bike as CANCELLED. Must be the owner.")
    public ResponseEntity<?> deleteBike(
            @Parameter(example = "1") @PathVariable(name = "id") Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        bikeService.deleteBike(id, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bike cancelled successfully");
        return ResponseEntity.ok(response);
    }
}
