package com.bikeexchange.controller;

import com.bikeexchange.dto.request.ListingComboRequest;

import com.bikeexchange.model.ListingCombo;
import com.bikeexchange.repository.ListingComboRepository;
import com.bikeexchange.service.service.OrderRuleConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/listing-combos")
@Tag(name = "Admin Combo Management", description = "Roles: Admin. APIs for managing listing packages/combos.")
@SecurityRequirement(name = "Bearer Token")
@PreAuthorize("hasRole('ADMIN')")
public class AdminComboController extends AdminBaseController {
    @Autowired
    private OrderRuleConfigService orderRuleConfigService;

    @Autowired
    private ListingComboRepository listingComboRepository;

    @GetMapping
    @Operation(summary = "Get all combos (including inactive ones)")
    public ResponseEntity<?> getAllCombos() {
        //SELECT
        return ok("Combos retrieved successfully", listingComboRepository.findAll());
    }

    @PostMapping
    @Operation(summary = "Create a new listing combo")
    public ResponseEntity<?> createCombo(@RequestBody ListingComboRequest request) {
        String validationError = validateRequest(request);
        if (validationError != null) {
            return badRequest(validationError);
        }

        ListingCombo combo = new ListingCombo();
        combo.setName(request.getName().trim());
        combo.setPointsCost(request.getPointsCost());
        combo.setPostLimit(request.getPostLimit());
        combo.setActive(request.isActive());
        //INSERT
        ListingCombo saved = listingComboRepository.save(combo);
        return created("Combo created successfully", saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing combo")
    public ResponseEntity<?> updateCombo(@PathVariable Long id, @RequestBody ListingComboRequest request) {
        String validationError = validateRequest(request);
        if (validationError != null) {
            return badRequest(validationError);
        }

        
        ListingCombo combo = listingComboRepository.findById(id)
                .orElse(null);
        if (combo == null) {
            return notFound("Combo not found");
        }
        combo.setName(request.getName().trim());
        combo.setPointsCost(request.getPointsCost());
        combo.setPostLimit(request.getPostLimit());
        combo.setActive(request.isActive());
        // UPDATE listing_combos SET name=?, points_cost=?, post_limit=?, active=? WHERE id=?;
        ListingCombo updated = listingComboRepository.save(combo);
        return ok("Combo updated successfully", updated);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a combo")
    public ResponseEntity<?> deactivateCombo(@PathVariable Long id) {
        // SELECT * FROM listing_combos WHERE id = ?;
        ListingCombo combo = listingComboRepository.findById(id)
                .orElse(null);
        if (combo == null) {
            return notFound("Combo not found");
        }
        combo.setActive(false);
        //UPDATE listing_combos SET active = false WHERE id = ?;
        ListingCombo updated = listingComboRepository.save(combo);
        return ok("Combo deactivated", updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete a combo")
    public ResponseEntity<?> deleteCombo(@PathVariable Long id) {
        
        ListingCombo combo = listingComboRepository.findById(id)
                .orElse(null);
        if (combo == null) {
            return notFound("Combo not found");
        }
        try {
            //DELETE
            listingComboRepository.delete(combo);
            return ok("Combo deleted permanently", null);
        } catch (DataIntegrityViolationException e) {
            return badRequest("Cannot delete combo because it is referenced by other data");
        }
    }

    private String validateRequest(ListingComboRequest request) {
        if (request == null) {
            return "Request body is required";
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return "Combo name is required";
        }
        if (request.getPointsCost() == null || request.getPointsCost() < 0) {
            return "pointsCost must be >= 0";
        }
        if (request.getPostLimit() == null || request.getPostLimit() <= 0) {
            return "postLimit must be > 0";
        }

        // Yêu cầu của cô: Giá combo phải rẻ hơn giá lẻ
        long singlePostPrice = orderRuleConfigService.getBikePostFee();
        long totalSinglePrice = singlePostPrice * request.getPostLimit();
        if (request.getPointsCost() >= totalSinglePrice) {
            return "Giá combo phải rẻ hơn mua lẻ từng bài (" + totalSinglePrice + " VND)";
        }

        return null;
    }
}
