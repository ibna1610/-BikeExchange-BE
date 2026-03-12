package com.bikeexchange.controller;

import com.bikeexchange.dto.request.CategoryRequest;
import com.bikeexchange.model.Category;
import com.bikeexchange.service.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categories")
@Tag(name = "Category Management", description = "APIs for managing bike categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody CategoryRequest categoryrequest) {
        if (categoryrequest.getName() == null || categoryrequest.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Category name is required"));
        }
        Category saved = categoryService.create(categoryrequest);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Category> result = categoryService.list(pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Category payload) {
        return categoryService.update(id, payload)
                .map(saved -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", saved);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        boolean ok = categoryService.delete(id);
        if (ok)
            return ResponseEntity.ok(Map.of("success", true));
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/bikes")
    public ResponseEntity<?> listBikesByCategory(@PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<com.bikeexchange.dto.response.BikeResponse> result = categoryService.listBikesByCategory(id, pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }
}