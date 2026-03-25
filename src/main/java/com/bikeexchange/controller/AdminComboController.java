package com.bikeexchange.controller;

import com.bikeexchange.dto.request.ListingComboRequest;
import com.bikeexchange.model.ListingCombo;
import com.bikeexchange.repository.ListingComboRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/listing-combos")
@Tag(name = "Admin Combo Management", description = "Roles: Admin. APIs for managing listing packages/combos.")
@SecurityRequirement(name = "Bearer Token")
@PreAuthorize("hasRole('ADMIN')")
public class AdminComboController {

    @Autowired
    private ListingComboRepository listingComboRepository;

    @GetMapping
    @Operation(summary = "Get all combos (including inactive ones)")
    public ResponseEntity<?> getAllCombos() {
        return ResponseEntity.ok(Map.of("success", true, "data", listingComboRepository.findAll()));
    }

    @PostMapping
    @Operation(summary = "Create a new listing combo")
    public ResponseEntity<?> createCombo(@RequestBody ListingComboRequest request) {
        ListingCombo combo = new ListingCombo();
        combo.setName(request.getName());
        combo.setPointsCost(request.getPointsCost());
        combo.setPostLimit(request.getPostLimit());
        combo.setActive(request.isActive());
        
        ListingCombo saved = listingComboRepository.save(combo);
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing combo")
    public ResponseEntity<?> updateCombo(@PathVariable Long id, @RequestBody ListingComboRequest request) {
        ListingCombo combo = listingComboRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Combo not found"));
        
        combo.setName(request.getName());
        combo.setPointsCost(request.getPointsCost());
        combo.setPostLimit(request.getPostLimit());
        combo.setActive(request.isActive());
        
        ListingCombo updated = listingComboRepository.save(combo);
        return ResponseEntity.ok(Map.of("success", true, "data", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete / deactivate a combo")
    public ResponseEntity<?> deleteCombo(@PathVariable Long id) {
        ListingCombo combo = listingComboRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Combo not found"));
        combo.setActive(false);
        listingComboRepository.save(combo);
        return ResponseEntity.ok(Map.of("success", true, "message", "Combo deactivated"));
    }
}
