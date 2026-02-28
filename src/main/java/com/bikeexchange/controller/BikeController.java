package com.bikeexchange.controller;

import com.bikeexchange.dto.request.BikeCreateRequest;
import com.bikeexchange.model.Bike;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.BikeService;
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
            @Parameter(description = "Search keyword for title, brand, or model", example = "Giant") @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by category ID", example = "1") @RequestParam(name = "category_id", required = false) Long categoryId,
            @Parameter(description = "Filter by statuses (repeat param or comma-separated). Example: status=VERIFIED&status=ACTIVE", example = "VERIFIED") @RequestParam(name = "status", required = false) java.util.List<String> statusParams,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bike> result = bikeService.searchBikesAdvanced(keyword, categoryId, statusParams, pageable);

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
            @Parameter(description = "ID of the bike", example = "1") @PathVariable Long id) {
        Bike bike = bikeService.getBikeById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", com.bikeexchange.dto.response.BikeResponse.fromEntity(bike));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a New Bike", description = "Create a new bike listing. If not authenticated, provide sellerId param for testing.")
    public ResponseEntity<?> createListing(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "sellerId", required = false) Long sellerIdParam,
            @RequestBody BikeCreateRequest request) {
        Long sellerId = currentUser != null ? currentUser.getId() : sellerIdParam;
        if (sellerId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "sellerId is required when not logged in"));
        }
        Bike bike = bikeService.createListing(sellerId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Listing created successfully in DRAFT state");
        response.put("data", com.bikeexchange.dto.response.BikeResponse.fromEntity(bike));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an Existing Bike", description = "Modify details of an existing bike listing. Must be the owner.")
    public ResponseEntity<?> updateListing(
            @Parameter(description = "ID of the bike", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody BikeCreateRequest request) {
        Bike bike = bikeService.updateListing(id, currentUser.getId(), request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Listing updated successfully");
        response.put("data", com.bikeexchange.dto.response.BikeResponse.fromEntity(bike));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete / Archive a Bike", description = "Soft delete or mark a bike listing as CANCELLED. Must be the owner.")
    public ResponseEntity<?> deleteListing(
            @Parameter(description = "ID of the bike", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        bikeService.deleteListing(id, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Listing cancelled successfully");
        return ResponseEntity.ok(response);
    }
}
