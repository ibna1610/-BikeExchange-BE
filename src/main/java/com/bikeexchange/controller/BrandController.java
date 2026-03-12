package com.bikeexchange.controller;

import com.bikeexchange.model.Brand;
import com.bikeexchange.service.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/brands")
@Tag(name = "Brand Management", description = "APIs for managing bike brands")
@SecurityRequirement(name = "Bearer Token")
public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping
    @Operation(summary = "List all brands", description = "Get a list of all bike brands available in the system.")
    public ResponseEntity<?> list() {
        List<Brand> result = brandService.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand detail", description = "Get details of a specific brand by ID.")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Brand brand = brandService.findById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", brand);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new brand", description = "Roles: Admin. Manually add a new bike brand.")
    public ResponseEntity<?> create(@RequestBody Brand brand) {
        Brand saved = brandService.create(brand);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a brand", description = "Roles: Admin. Update brand name or description.")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Brand brand) {
        Brand saved = brandService.update(id, brand);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a brand", description = "Roles: Admin. Delete a brand ONLY IF it has no associated bikes.")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        brandService.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Brand deleted successfully");
        return ResponseEntity.ok(response);
    }
}
