package com.bikeexchange.service;

import com.bikeexchange.model.Bike;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.BikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BikeService {
    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private UserService userService;

    public Bike createBike(Bike bike) {
        if (bike.getSeller() == null || bike.getSeller().getId() == null) {
            throw new IllegalArgumentException("Seller information is required");
        }
        User seller = userService.getUserById(bike.getSeller().getId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        bike.setSeller(seller);
        bike.setStatus(Bike.BikeStatus.AVAILABLE);
        return bikeRepository.save(bike);
    }

    public Optional<Bike> getBikeById(Long bikeId) {
        Optional<Bike> bike = bikeRepository.findById(bikeId);
        bike.ifPresent(b -> {
            b.setViews(b.getViews() + 1);
            bikeRepository.save(b);
        });
        return bike;
    }

    public Page<Bike> getAvailableBikes(Pageable pageable) {
        return bikeRepository.findByStatus(Bike.BikeStatus.AVAILABLE, pageable);
    }

    public Page<Bike> getSellerBikes(Long sellerId, Pageable pageable) {
        return bikeRepository.findBySellerId(sellerId, pageable);
    }

    public Page<Bike> searchBikes(String keyword, Pageable pageable) {
        return bikeRepository.searchAvailableBikes(keyword, pageable);
    }

    public Page<Bike> filterBikes(Long minPrice, Long maxPrice, Integer minYear, Pageable pageable) {
        return bikeRepository.filterBikes(minPrice, maxPrice, minYear, pageable);
    }

    public Bike updateBike(Long bikeId, Bike updatedBike) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));
        bike.setTitle(updatedBike.getTitle());
        bike.setDescription(updatedBike.getDescription());
        bike.setPrice(updatedBike.getPrice());
        bike.setCondition(updatedBike.getCondition());
        bike.setFeatures(updatedBike.getFeatures());
        bike.setLocation(updatedBike.getLocation());
        return bikeRepository.save(bike);
    }

    public void deleteBike(Long bikeId) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));
        bike.setStatus(Bike.BikeStatus.ARCHIVED);
        bikeRepository.save(bike);
    }

    public Bike markBikeAsSold(Long bikeId) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));
        bike.setStatus(Bike.BikeStatus.SOLD);
        return bikeRepository.save(bike);
    }

    public Bike reserveBike(Long bikeId) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));
        bike.setStatus(Bike.BikeStatus.RESERVED);
        return bikeRepository.save(bike);
    }

    public List<Bike> getBikesByType(String bikeType) {
        return bikeRepository.findByBikeTypeAndStatus(bikeType, Bike.BikeStatus.AVAILABLE);
    }
}
