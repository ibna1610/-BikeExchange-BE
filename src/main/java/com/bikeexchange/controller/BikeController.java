package com.bikeexchange.controller;

import com.bikeexchange.model.Bike;
import com.bikeexchange.service.BikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bikes")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BikeController {
    @Autowired
    private BikeService bikeService;

    @PostMapping
    public ResponseEntity<Bike> createBike(@RequestBody Bike bike) {
        try {
            Bike createdBike = bikeService.createBike(bike);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBike);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{bikeId}")
    public ResponseEntity<Bike> getBikeById(@PathVariable Long bikeId) {
        return bikeService.getBikeById(bikeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<Bike>> getAvailableBikes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bike> bikes = bikeService.getAvailableBikes(pageable);
        return ResponseEntity.ok(bikes);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<Bike>> getSellerBikes(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bike> bikes = bikeService.getSellerBikes(sellerId, pageable);
        return ResponseEntity.ok(bikes);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Bike>> searchBikes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bike> bikes = bikeService.searchBikes(keyword, pageable);
        return ResponseEntity.ok(bikes);
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<Bike>> filterBikes(
            @RequestParam Long minPrice,
            @RequestParam Long maxPrice,
            @RequestParam Integer minYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bike> bikes = bikeService.filterBikes(minPrice, maxPrice, minYear, pageable);
        return ResponseEntity.ok(bikes);
    }

    @GetMapping("/type/{bikeType}")
    public ResponseEntity<?> getBikesByType(@PathVariable String bikeType) {
        return ResponseEntity.ok(bikeService.getBikesByType(bikeType));
    }

    @PutMapping("/{bikeId}")
    public ResponseEntity<Bike> updateBike(@PathVariable Long bikeId, @RequestBody Bike bike) {
        try {
            Bike updatedBike = bikeService.updateBike(bikeId, bike);
            return ResponseEntity.ok(updatedBike);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{bikeId}")
    public ResponseEntity<?> deleteBike(@PathVariable Long bikeId) {
        try {
            bikeService.deleteBike(bikeId);
            return ResponseEntity.ok("Bike deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{bikeId}/mark-sold")
    public ResponseEntity<Bike> markBikeAsSold(@PathVariable Long bikeId) {
        try {
            Bike bike = bikeService.markBikeAsSold(bikeId);
            return ResponseEntity.ok(bike);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
