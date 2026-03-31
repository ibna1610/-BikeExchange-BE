package com.bikeexchange.controller;

import com.bikeexchange.model.Brand;
import com.bikeexchange.model.Category;
import com.bikeexchange.repository.BrandRepository;
import com.bikeexchange.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Catalog", description = "6.3 Quản lý danh mục (loại xe, thương hiệu)")
@SecurityRequirement(name = "Bearer Token")
public class AdminCatalogController extends AdminBaseController {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    // Removed Component logic

    // ── Categories ──────────────────────────────────────────────────────────

    @GetMapping("/categories")
    @Operation(summary = "Danh sách loại xe")
    public ResponseEntity<?> listCategories() {
        return ok("Categories retrieved successfully", categoryRepository.findAll());
    }

    @PostMapping("/categories")
    @Operation(summary = "Tạo loại xe")
    public ResponseEntity<?> createCategory(@RequestBody Category payload) {
        if (payload.getName() == null || payload.getName().isBlank())
            return badRequest("Category name is required");
        if (categoryRepository.findByName(payload.getName()).isPresent())
            return badRequest("Category already exists");
        return created("Category created successfully", categoryRepository.save(payload));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Cập nhật loại xe")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category payload) {
        return categoryRepository.findById(id).<ResponseEntity<?>>map(cat -> {
            if (payload.getName() != null && !payload.getName().isBlank()) cat.setName(payload.getName());
            cat.setDescription(payload.getDescription());
            cat.setImgUrl(payload.getImgUrl());
            return ok("Category updated successfully", categoryRepository.save(cat));
        }).orElseGet(() -> notFound("Category not found"));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Xóa loại xe")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) return notFound("Category not found");
        categoryRepository.deleteById(id);
        return ok("Category deleted", Map.of("id", id));
    }

    // ── Brands ──────────────────────────────────────────────────────────────

    @GetMapping("/brands")
    @Operation(summary = "Danh sách thương hiệu")
    public ResponseEntity<?> listBrands() {
        return ok("Brands retrieved successfully", brandRepository.findAll());
    }

    @PostMapping("/brands")
    @Operation(summary = "Tạo thương hiệu")
    public ResponseEntity<?> createBrand(@RequestBody Brand payload) {
        if (payload.getName() == null || payload.getName().isBlank())
            return badRequest("Brand name is required");
        if (brandRepository.findByName(payload.getName()).isPresent())
            return badRequest("Brand already exists");
        return created("Brand created successfully", brandRepository.save(payload));
    }

    @PutMapping("/brands/{id}")
    @Operation(summary = "Cập nhật thương hiệu")
    public ResponseEntity<?> updateBrand(@PathVariable Long id, @RequestBody Brand payload) {
        return brandRepository.findById(id).<ResponseEntity<?>>map(brand -> {
            if (payload.getName() != null && !payload.getName().isBlank()) brand.setName(payload.getName());
            brand.setDescription(payload.getDescription());
            return ok("Brand updated successfully", brandRepository.save(brand));
        }).orElseGet(() -> notFound("Brand not found"));
    }

    @DeleteMapping("/brands/{id}")
    @Operation(summary = "Xóa thương hiệu")
    public ResponseEntity<?> deleteBrand(@PathVariable Long id) {
        if (!brandRepository.existsById(id)) return notFound("Brand not found");
        brandRepository.deleteById(id);
        return ok("Brand deleted", Map.of("id", id));
    }

    // ── Components ───────────────────────────────────────────────────────────

    // Removed Component logic
}
