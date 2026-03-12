package com.bikeexchange.service.service;

import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.Brand;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private BikeRepository bikeRepository;

    public List<Brand> findAll() {
        return brandRepository.findAll();
    }

    public Brand findById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }

    @Transactional
    public Brand create(Brand brand) {
        if (brandRepository.findByName(brand.getName()).isPresent()) {
            throw new IllegalArgumentException("Brand with name '" + brand.getName() + "' already exists");
        }
        return brandRepository.save(brand);
    }

    @Transactional
    public Brand update(Long id, Brand payload) {
        Brand existing = findById(id);
        
        if (payload.getName() != null && !payload.getName().equals(existing.getName())) {
            if (brandRepository.findByName(payload.getName()).isPresent()) {
                throw new IllegalArgumentException("Brand with name '" + payload.getName() + "' already exists");
            }
            existing.setName(payload.getName());
        }
        
        if (payload.getDescription() != null) {
            existing.setDescription(payload.getDescription());
        }
        
        return brandRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = findById(id);
        
        long count = bikeRepository.countByBrandId(id);
        if (count > 0) {
            throw new IllegalStateException("Cannot delete brand '" + brand.getName() + "' because it is associated with " + count + " bike(s).");
        }
        
        brandRepository.delete(brand);
    }
}
