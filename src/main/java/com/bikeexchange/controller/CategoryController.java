package com.bikeexchange.controller;

import com.bikeexchange.model.Category;
import com.bikeexchange.model.Bike;
import com.bikeexchange.repository.CategoryRepository;
import com.bikeexchange.repository.BikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Category name is required"));
        }
        Category saved = categoryRepository.save(category);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> result = categoryRepository.findAll(pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Category payload) {
        return categoryRepository.findById(id)
                .map(existing -> {
                    existing.setName(payload.getName() != null ? payload.getName() : existing.getName());
                    existing.setDescription(payload.getDescription() != null ? payload.getDescription() : existing.getDescription());
                    Category saved = categoryRepository.save(existing);
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
        return categoryRepository.findById(id)
                .map(existing -> {
                    categoryRepository.delete(existing);
                    return ResponseEntity.ok(Map.of("success", true));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/bikes")
    public ResponseEntity<?> listBikesByCategory(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bike> result = bikeRepository.findByCategories_Id(id, pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }
}
