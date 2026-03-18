package com.bikeexchange.controller;

import com.bikeexchange.dto.response.ImageUploadResponse;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/upload")
@Tag(name = "Image Upload", description = "Endpoints for uploading images to Cloudinary")
@SecurityRequirement(name = "Bearer Token")
public class UploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Upload bike image with seller information
     * When seller uploads bike images, include seller's address and shop name
     */
    @PostMapping("/bike-image")
    @PreAuthorize("isAuthenticated() and hasRole('SELLER')")
    @Operation(summary = "Upload Bike Image", description = "Upload bike listing image with seller location info")
    public ResponseEntity<?> uploadBikeImage(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bike_id", required = false) Long bikeId) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File is empty"
                ));
            }

            // Check file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File size exceeds 5MB limit"
                ));
            }

            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Only image files are allowed"
                ));
            }

            // Get seller information
            User seller = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Seller not found"));

            // Upload to Cloudinary with bikes folder + seller ID subfolder
            String folder = "bikes/seller-" + seller.getId();
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, folder);

            // Get seller address (shop location)
            String sellerAddress = seller.getAddress() != null ? seller.getAddress() : "Not specified";
            String sellerName = seller.getShopName() != null ? seller.getShopName() : seller.getFullName();
            String thumbnailUrl = cloudinaryService.getThumbnailUrl((String) uploadResult.get("public_id"));

            // Build response with seller info
            ImageUploadResponse response = ImageUploadResponse.builder()
                    .url((String) uploadResult.get("secure_url"))
                    .publicId((String) uploadResult.get("public_id"))
                    .fileSize((Long) uploadResult.get("bytes"))
                    .uploadedAt(LocalDateTime.now().toString())
                    .sellerAddress(sellerAddress)
                    .sellerName(sellerName)
                    .cloudinaryFolder(folder)
                    .thumbnailUrl(thumbnailUrl)
                    .build();

            log.info("Bike image uploaded by seller {}: {}, Address: {}", 
                    seller.getId(), sellerName, sellerAddress);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Image uploaded successfully with seller location");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("Error uploading bike image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to upload image: " + e.getMessage()
            ));
        }
    }

    /**
     * Upload multiple bike images
     */
    @PostMapping("/bike-images")
    @PreAuthorize("isAuthenticated() and hasRole('SELLER')")
    @Operation(summary = "Upload Multiple Bike Images", description = "Upload multiple bike images at once")
    public ResponseEntity<?> uploadMultipleBikeImages(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            if (files.length == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No files provided"
                ));
            }

            // Get seller information
            User seller = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Seller not found"));

            String sellerAddress = seller.getAddress() != null ? seller.getAddress() : "Not specified";
            String sellerName = seller.getShopName() != null ? seller.getShopName() : seller.getFullName();
            String folder = "bikes/seller-" + seller.getId();

            java.util.List<ImageUploadResponse> uploadedImages = new java.util.ArrayList<>();

            for (MultipartFile file : files) {
                if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
                    continue;
                }

                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    continue;
                }

                try {
                    Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, folder);
                    String thumbnailUrl = cloudinaryService.getThumbnailUrl((String) uploadResult.get("public_id"));

                    ImageUploadResponse uploadResponse = ImageUploadResponse.builder()
                            .url((String) uploadResult.get("secure_url"))
                            .publicId((String) uploadResult.get("public_id"))
                            .fileSize((Long) uploadResult.get("bytes"))
                            .uploadedAt(LocalDateTime.now().toString())
                            .sellerAddress(sellerAddress)
                            .sellerName(sellerName)
                            .cloudinaryFolder(folder)
                            .thumbnailUrl(thumbnailUrl)
                            .build();

                    uploadedImages.add(uploadResponse);
                } catch (IOException e) {
                    log.warn("Failed to upload file: {}", e.getMessage());
                }
            }

            if (uploadedImages.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No valid images could be uploaded"
                ));
            }

            log.info("Multiple bike images uploaded by seller {}: {}, Address: {}, Count: {}", 
                    seller.getId(), sellerName, sellerAddress, uploadedImages.size());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Images uploaded successfully with seller location");
            result.put("count", uploadedImages.size());
            result.put("data", uploadedImages);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error uploading multiple bike images: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to upload images: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete uploaded image from Cloudinary
     */
    @DeleteMapping("/{publicId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete Uploaded Image", description = "Remove image from Cloudinary (public_id encoded)")
    public ResponseEntity<?> deleteImage(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String publicId) {
        
        try {
            // Decode the public_id from URL parameter
            String decodedPublicId = java.net.URLDecoder.decode(publicId, java.nio.charset.StandardCharsets.UTF_8);

            cloudinaryService.deleteFile(decodedPublicId);
            
            log.info("Image deleted by user {}: {}", currentUser.getId(), decodedPublicId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Image deleted successfully");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error deleting image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to delete image: " + e.getMessage()
            ));
        }
    }

    /**
     * Get seller's address (for bike listing)
     */
    @GetMapping("/seller-info")
    @PreAuthorize("isAuthenticated() and hasRole('SELLER')")
    @Operation(summary = "Get Seller Location Info", description = "Get seller's address and shop name for uploading bikes")
    public ResponseEntity<?> getSellerInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        
        try {
            User seller = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Seller not found"));

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", Map.of(
                "seller_id", seller.getId(),
                "seller_name", seller.getShopName() != null ? seller.getShopName() : seller.getFullName(),
                "seller_address", seller.getAddress() != null ? seller.getAddress() : "Not specified",
                "seller_phone", seller.getPhone(),
                "seller_email", seller.getEmail()
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting seller info: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to get seller info: " + e.getMessage()
            ));
        }
    }
}
