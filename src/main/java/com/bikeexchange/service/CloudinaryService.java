package com.bikeexchange.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Upload file to Cloudinary
     * @param file MultipartFile to upload
     * @param folder Folder in Cloudinary (e.g., "bikes", "profiles")
     * @return Map with upload response containing url and public_id
     * @throws IOException if upload fails
     */
    public Map<String, Object> uploadFile(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto"
            );

            return cloudinary.uploader().upload(file.getBytes(), uploadParams);
        } catch (IOException e) {
            log.error("Error uploading file to Cloudinary: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Upload file from URL to Cloudinary
     * @param fileUrl URL of the file to upload
     * @param folder Folder in Cloudinary
     * @return Map with upload response
     * @throws IOException if upload fails
     */
    public Map<String, Object> uploadFileFromUrl(String fileUrl, String folder) throws IOException {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto"
            );

            return cloudinary.uploader().upload(fileUrl, uploadParams);
        } catch (IOException e) {
            log.error("Error uploading file from URL to Cloudinary: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete file from Cloudinary by public_id
     * @param publicId Public ID of the file to delete
     * @return Deletion response
     * @throws IOException if deletion fails
     */
    public Map<String, Object> deleteFile(String publicId) throws IOException {
        try {
            return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Error deleting file from Cloudinary (publicId: {}): {}", publicId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get secure URL for a resource
     * @param publicId Public ID of the resource
     * @return Secure HTTPS URL
     */
    public String getSecureUrl(String publicId) {
        return cloudinary.url()
                .secure(true)
                .generate(publicId);
    }

    /**
     * Get optimized image URL with transformations
     * @param publicId Public ID of the image
    /**
     * Get thumbnail URL
     * @param publicId Public ID of the image
     * @return Thumbnail URL placeholder
     */
    public String getThumbnailUrl(String publicId) {
        // TODO: Implement image optimization with Cloudinary transformation API
        // For now, return the public ID
        return publicId;
    }
}
