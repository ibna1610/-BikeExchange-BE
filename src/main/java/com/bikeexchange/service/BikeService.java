package com.bikeexchange.service;

import com.bikeexchange.dto.request.BikeCreateRequest;
import com.bikeexchange.dto.request.BikeMediaRequest;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.BikeMedia;
import com.bikeexchange.model.Brand;
import com.bikeexchange.model.Category;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.BrandRepository;
import com.bikeexchange.repository.CategoryRepository;
import com.bikeexchange.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BikeService {

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private HistoryService historyService;

    public Page<Bike> searchBikes(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return bikeRepository.searchAvailableBikes(keyword, pageable);
        }
        return bikeRepository.findByStatus(Bike.BikeStatus.ACTIVE, pageable);
    }

    public Page<Bike> searchBikesByCategory(Long categoryId, Pageable pageable) {
        return bikeRepository.findByCategories_Id(categoryId, pageable);
    }

    public Bike getBikeById(Long id) {
        return bikeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bike not found with id: " + id));
    }

    public Bike createListing(Long sellerId, BikeCreateRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        Bike bike = new Bike();
        bike.setSeller(seller);
        bike.setTitle(request.getTitle());
        bike.setDescription(request.getDescription());

        Brand brandEntity = brandRepository.findByName(request.getBrand())
                .orElseGet(() -> {
                    Brand nb = new Brand();
                    nb.setName(request.getBrand());
                    return brandRepository.save(nb);
                });
        bike.setBrand(brandEntity);

        bike.setModel(request.getModel());
        bike.setYear(request.getYear());
        bike.setPricePoints(request.getPricePoints());
        bike.setCondition(request.getCondition());
        bike.setBikeType(request.getBikeType());
        // Default status
        bike.setStatus(Bike.BikeStatus.DRAFT);
        bike.setInspectionStatus(Bike.InspectionStatus.NONE);
        bike.setCreatedAt(LocalDateTime.now());
        bike.setMileage(0);
        bike.setLocation("Not specified");

        // Handle Media
        if (request.getMedia() != null && !request.getMedia().isEmpty()) {
            for (int i = 0; i < request.getMedia().size(); i++) {
                BikeMediaRequest mr = request.getMedia().get(i);
                BikeMedia bm = new BikeMedia();
                bm.setBike(bike);
                bm.setUrl(mr.getUrl());
                bm.setType(BikeMedia.MediaType.valueOf(mr.getType().toUpperCase()));
                bm.setSortOrder(mr.getSortOrder() != null ? mr.getSortOrder() : i);
                bike.getMedia().add(bm);
            }
        }

        // Handle Categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>();
            for (Long cid : request.getCategoryIds()) {
                Category cat = categoryRepository.findById(cid)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + cid));
                categories.add(cat);
            }
            bike.setCategories(categories);
        }

        Bike saved = bikeRepository.save(bike);
        historyService.log("bike", saved.getId(), "created", seller.getId(), null);
        return saved;
    }

    public Bike updateListing(Long listingId, Long sellerId, BikeCreateRequest request) {
        Bike bike = getBikeById(listingId);

        if (!bike.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Only the seller can update this listing");
        }

        if (bike.getStatus() == Bike.BikeStatus.RESERVED || bike.getStatus() == Bike.BikeStatus.SOLD) {
            throw new IllegalStateException("Cannot update a listing that is reserved or sold");
        }

        bike.setTitle(request.getTitle());
        bike.setDescription(request.getDescription());

        Brand brandEntity = brandRepository.findByName(request.getBrand())
                .orElseGet(() -> {
                    Brand nb = new Brand();
                    nb.setName(request.getBrand());
                    return brandRepository.save(nb);
                });
        bike.setBrand(brandEntity);

        bike.setModel(request.getModel());
        bike.setYear(request.getYear());
        bike.setPricePoints(request.getPricePoints());
        bike.setCondition(request.getCondition());
        bike.setBikeType(request.getBikeType());
        bike.setUpdatedAt(LocalDateTime.now());

        // Handle Media logic
        if (request.getMedia() != null) {
            bike.getMedia().clear(); // Orphan removal will delete old rows
            for (int i = 0; i < request.getMedia().size(); i++) {
                BikeMediaRequest mr = request.getMedia().get(i);
                BikeMedia bm = new BikeMedia();
                bm.setBike(bike);
                bm.setUrl(mr.getUrl());
                bm.setType(BikeMedia.MediaType.valueOf(mr.getType().toUpperCase()));
                bm.setSortOrder(mr.getSortOrder() != null ? mr.getSortOrder() : i);
                bike.getMedia().add(bm);
            }
        }

        // Handle Categories
        if (request.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>();
            for (Long cid : request.getCategoryIds()) {
                Category cat = categoryRepository.findById(cid)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + cid));
                categories.add(cat);
            }
            bike.setCategories(categories);
        }

        Bike saved = bikeRepository.save(bike);
        historyService.log("bike", saved.getId(), "updated", sellerId, null);
        return saved;
    }

    public void deleteListing(Long listingId, Long sellerId) {
        Bike bike = getBikeById(listingId);

        if (!bike.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Only the seller can delete this listing");
        }

        if (bike.getStatus() == Bike.BikeStatus.RESERVED) {
            throw new IllegalStateException("Cannot delete a reserved listing");
        }

        // Soft delete or just update status to CANCELLED
        bike.setStatus(Bike.BikeStatus.CANCELLED);
        bike.setUpdatedAt(LocalDateTime.now());
        bikeRepository.save(bike);
        historyService.log("bike", bike.getId(), "cancelled", sellerId, null);
    }
}
