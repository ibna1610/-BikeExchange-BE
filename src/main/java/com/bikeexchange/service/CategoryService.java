package com.bikeexchange.service;

import com.bikeexchange.dto.response.BikeResponse;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Category;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BikeRepository bikeRepository;

    public Category create(Category category) {
        return categoryRepository.save(category);
    }

    public Page<Category> list(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public Optional<Category> update(Long id, Category payload) {
        return categoryRepository.findById(id).map(existing -> {
            existing.setName(payload.getName() != null ? payload.getName() : existing.getName());
            existing.setDescription(
                    payload.getDescription() != null ? payload.getDescription() : existing.getDescription());
            existing.setImgUrl(payload.getImgUrl() != null ? payload.getImgUrl() : existing.getImgUrl());
            return categoryRepository.save(existing);
        });
    }

    public boolean delete(Long id) {
        return categoryRepository.findById(id).map(existing -> {
            categoryRepository.delete(existing);
            return true;
        }).orElse(false);
    }

    public Page<BikeResponse> listBikesByCategory(Long categoryId, Pageable pageable) {
        Page<Bike> bikes = bikeRepository.findByCategories_Id(categoryId, pageable);
        return bikes.map(BikeResponse::fromEntity);
    }
}
