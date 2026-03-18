package com.bikeexchange.service;

import com.bikeexchange.dto.request.BikeCreateRequest;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.BikeMedia;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BikeUploadService {

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * Create bike with images from seller's upload
     * This method ensures bike is created with seller's location info
     */
    @Transactional
    public Bike createBikeWithImages(Long sellerId, BikeCreateRequest request, List<MultipartFile> imageFiles) throws IOException {
        // Get seller
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with ID: " + sellerId));

        // Validate that seller has address
        if (seller.getAddress() == null || seller.getAddress().isBlank()) {
            throw new IllegalArgumentException("Seller must have a valid address to list bikes");
        }

        // Create bike with seller's address as location
        Bike bike = new Bike();
        bike.setSeller(seller);
        bike.setTitle(request.getTitle());
        bike.setDescription(request.getDescription());
        bike.setModel(request.getModel());
        bike.setYear(request.getYear());
        bike.setPricePoints(request.getPricePoints());
        bike.setCondition(request.getCondition());
        bike.setBikeType(request.getBikeType());
        bike.setFrameSize(request.getFrameSize());
        
        // Set location from seller's address
        bike.setLocation(seller.getAddress());
        
        bike.setStatus(Bike.BikeStatus.ACTIVE);
        bike.setInspectionStatus(Bike.InspectionStatus.NONE);
        bike.setCreatedAt(LocalDateTime.now());
        bike.setMileage(0);
        bike.setViews(0);

        // Upload images to Cloudinary with seller folder
        String cloudinaryFolder = "bikes/seller-" + sellerId;
        int sortOrder = 0;

        for (MultipartFile imageFile : imageFiles) {
            if (imageFile.isEmpty() || imageFile.getSize() > 5 * 1024 * 1024) {
                log.warn("Skipping invalid image file: {}", imageFile.getOriginalFilename());
                continue;
            }

            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Skipping non-image file: {}", imageFile.getOriginalFilename());
                continue;
            }

            try {
                // Upload to Cloudinary
                Map<String, Object> uploadResult = cloudinaryService.uploadFile(imageFile, cloudinaryFolder);

                // Create BikeMedia entry with Cloudinary URL
                BikeMedia bikeMedia = new BikeMedia();
                bikeMedia.setBike(bike);
                bikeMedia.setUrl((String) uploadResult.get("secure_url"));
                // Note: publicId can be added to BikeMedia if deletion feature is needed later
                bikeMedia.setType(BikeMedia.MediaType.IMAGE);
                bikeMedia.setSortOrder(sortOrder++);

                bike.getMedia().add(bikeMedia);

                log.info("Image uploaded for bike from seller {}: {} (Size: {} bytes)",
                        sellerId, imageFile.getOriginalFilename(), imageFile.getSize());

            } catch (IOException e) {
                log.error("Failed to upload image {}: {}", imageFile.getOriginalFilename(), e.getMessage());
                throw e;
            }
        }

        // Validate that at least one image was uploaded
        if (bike.getMedia().isEmpty()) {
            throw new IllegalArgumentException("At least one valid image is required to list a bike");
        }

        // Set featured image (first one)
        Bike savedBike = bikeRepository.save(bike);
        
        log.info("Bike created with location from seller {}: {} - Address: {}, Images: {}", 
                sellerId, request.getTitle(), seller.getAddress(), savedBike.getMedia().size());

        return savedBike;
    }

    /**
     * Update bike location from seller's address whenever seller address changes
     */
    @Transactional
    public void updateBikesLocationFromSellerAddress(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with ID: " + sellerId));

        if (seller.getAddress() == null || seller.getAddress().isBlank()) {
            return;
        }

        // Find all bikes by this seller and update their location
        List<Bike> sellerBikes = bikeRepository.findBySellerId(sellerId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        for (Bike bike : sellerBikes) {
            if (!bike.getLocation().equals(seller.getAddress())) {
                bike.setLocation(seller.getAddress());
                bikeRepository.save(bike);
                log.info("Updated bike {} location to seller's new address: {}", bike.getId(), seller.getAddress());
            }
        }
    }

    /**
     * Get seller information for bike location display
     */
    public Map<String, Object> getSellerLocationInfo(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with ID: " + sellerId));

        return Map.of(
                "seller_id", seller.getId(),
                "seller_name", seller.getShopName() != null ? seller.getShopName() : seller.getFullName(),
                "address", seller.getAddress() != null ? seller.getAddress() : "Not specified",
                "phone", seller.getPhone(),
                "email", seller.getEmail(),
                "shop_description", seller.getShopDescription() != null ? seller.getShopDescription() : ""
        );
    }
}
