package com.bikeexchange.controller;

import com.bikeexchange.dto.request.BikeCreateRequest;
import com.bikeexchange.model.Bike;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.service.BikeService;
import com.bikeexchange.service.BikeUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/bikes")
@Tag(name = "Bike Listing Management", description = "APIs for managing bike listings, updating status, and searching (formerly listings)")
@SecurityRequirement(name = "Bearer Token")
public class BikeController {

    @Autowired
    private BikeService bikeService;

    @Autowired
    private BikeUploadService bikeUploadService;

    @GetMapping
    @Operation(summary = "Search and Filter Bikes", description = "Retrieve a paginated list of bikes. Filters: keyword, category_id, status (repeat param or comma-separated).")
    public ResponseEntity<?> getListings(
             @RequestParam(name = "keyword", required = false) String keyword,
             @RequestParam(name = "category_id", required = false) Long categoryId,
             @RequestParam(name = "status", required = false) String status,
             @RequestParam(name = "price_min", required = false) Long priceMin,
             @RequestParam(name = "price_max", required = false) Long priceMax,
             @RequestParam(name = "brand_id", required = false) Long brandId,
             @RequestParam(name = "min_year", required = false) Integer minYear,
             @RequestParam(name = "frame_size", required = false) String frameSize,
             @RequestParam(name = "seller_id", required = false) Long sellerId,
             @RequestParam(name = "sort_by_rating", defaultValue = "false") boolean sortByRating) {
        // Use a large size to return "all" results (e.g., 1000)
        Pageable pageable = PageRequest.of(0, 1000);
        Page<Bike> result = bikeService.searchBikesAdvanced(keyword, categoryId, status, priceMin, priceMax,
                brandId, minYear, frameSize, sellerId, sortByRating, pageable);

        java.util.List<com.bikeexchange.dto.response.BikeResponse> dtoList = result.getContent()
                .stream()
                .map(com.bikeexchange.dto.response.BikeResponse::fromEntity)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", dtoList);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Bike Details", description = "Fetch complete details of a single bike listing by its ID")
    public ResponseEntity<?> getListing(
             @PathVariable(name = "id") Long id) {
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

    /**
     * Create bike with images - seller must upload at least 1 image
     * Location will be automatically filled from seller's address
     */
    @PostMapping("/with-images")
    @PreAuthorize("isAuthenticated() and hasRole('SELLER')")
    @Operation(summary = "Create Bike with Images", description = "Create a new bike with images. Seller location is automatically filled from their address.")
    public ResponseEntity<?> createBikeWithImages(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("brand_id") Long brandId,
            @RequestParam("model") String model,
            @RequestParam("year") Integer year,
            @RequestParam("price_points") Long pricePoints,
            @RequestParam("condition") String condition,
            @RequestParam("bike_type") String bikeType,
            @RequestParam(value = "frame_size", required = false) String frameSize,
            @RequestParam(value = "category_ids", required = false) java.util.List<Long> categoryIds,
            @RequestParam("images") MultipartFile[] images) {
        
        try {
            Long sellerId = currentUser.getId();

            // Validate images
            if (images == null || images.length == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "At least one image is required"
                ));
            }

            // Create bike request
            BikeCreateRequest request = new BikeCreateRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setBrandId(brandId);
            request.setModel(model);
            request.setYear(year);
            request.setPricePoints(pricePoints);
            request.setCondition(condition);
            request.setBikeType(bikeType);
            request.setFrameSize(frameSize);
            request.setCategoryIds(categoryIds);

            // Create bike with images (location filled from seller's address)
            Bike bike = bikeUploadService.createBikeWithImages(sellerId, request, java.util.Arrays.asList(images));

            log.info("Bike created with {} images by seller {}: {}", 
                    bike.getMedia().size(), sellerId, title);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bike created successfully with " + bike.getMedia().size() + " images and seller location");
            response.put("data", com.bikeexchange.dto.response.BikeResponse.fromEntity(bike));
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error creating bike with images: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to create bike: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an Existing Bike", description = "Modify details of an existing bike. Must be the owner.")
    public ResponseEntity<?> updateBike(
             @PathVariable(name = "id") Long id,
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
    @Operation(summary = "Delete / Archive a Bike", description = "Soft delete or mark a bike as CANCELLED. Must be the owner or an Admin.")
    public ResponseEntity<?> deleteBike(
             @PathVariable(name = "id") Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        bikeService.deleteBike(id, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bike cancelled successfully");
        return ResponseEntity.ok(response);
    }
}
