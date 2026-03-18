package com.bikeexchange.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
public class CloudinaryConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        String normalizedCloudName = cloudName == null ? "" : cloudName.trim();
        String normalizedApiKey = apiKey == null ? "" : apiKey.trim();
        String normalizedApiSecret = apiSecret == null ? "" : apiSecret.trim();
        String normalizedCloudinaryUrl = cloudinaryUrl == null ? "" : cloudinaryUrl.trim();

        if (normalizedCloudName.isBlank()) {
            normalizedCloudName = readDotEnvValue("CLOUDINARY_CLOUD_NAME").orElse("");
        }
        if (normalizedApiKey.isBlank()) {
            normalizedApiKey = readDotEnvValue("CLOUDINARY_API_KEY").orElse("");
        }
        if (normalizedApiSecret.isBlank()) {
            normalizedApiSecret = readDotEnvValue("CLOUDINARY_API_SECRET").orElse("");
        }
        if (normalizedCloudinaryUrl.isBlank()) {
            normalizedCloudinaryUrl = readDotEnvValue("CLOUDINARY_URL").orElse("");
        }

        if (!normalizedCloudName.isBlank() && !normalizedApiKey.isBlank() && !normalizedApiSecret.isBlank()) {
            log.info("Cloudinary config source: split credentials");
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", normalizedCloudName);
            config.put("api_key", normalizedApiKey);
            config.put("api_secret", normalizedApiSecret);
            return new Cloudinary(config);
        }

        if (!normalizedCloudinaryUrl.isBlank()) {
            log.info("Cloudinary config source: CLOUDINARY_URL");
            return new Cloudinary(normalizedCloudinaryUrl);
        }

        log.warn("Cloudinary config is empty; uploads will fail. Please set CLOUDINARY_* values.");
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", normalizedCloudName);
        config.put("api_key", normalizedApiKey);
        config.put("api_secret", normalizedApiSecret);
        return new Cloudinary(config);
    }

    private Optional<String> readDotEnvValue(String key) {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return Optional.empty();
        }
        try {
            return Files.readAllLines(envPath).stream()
                    .map(String::trim)
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .map(line -> line.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .filter(parts -> parts[0].trim().equals(key))
                    .map(parts -> parts[1].trim())
                    .map(value -> {
                        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                            return value.substring(1, value.length() - 1);
                        }
                        return value;
                    })
                    .findFirst();
        } catch (IOException e) {
            log.warn("Cannot read .env file for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }
}
