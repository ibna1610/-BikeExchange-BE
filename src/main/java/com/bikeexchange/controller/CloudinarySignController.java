package com.bikeexchange.controller;

import com.bikeexchange.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Backend-proxy upload controller
 * Frontend uploads files to backend, backend uploads to Cloudinary
 * More secure than frontend direct upload
 */
@RestController
@RequestMapping("/api/cloudinary")
public class CloudinarySignController {

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * Backend proxy upload endpoint
     * Frontend sends file, backend uploads to Cloudinary
     * Returns Cloudinary URL
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Upload through backend to Cloudinary
            String folder = "bikes/uploads/" + System.currentTimeMillis();
            Map<String, Object> uploadResponse = cloudinaryService.uploadFile(file, folder);
            
            // Extract URL from Cloudinary response
            String url = (String) uploadResponse.getOrDefault("secure_url", uploadResponse.get("url"));
            String publicId = (String) uploadResponse.get("public_id");
            
            if (url == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No URL in Cloudinary response"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("url", url);
            response.put("publicId", publicId);
            response.put("message", "Upload successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
