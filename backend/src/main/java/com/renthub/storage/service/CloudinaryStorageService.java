package com.renthub.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service("cloudinaryStorage")
@Slf4j
public class CloudinaryStorageService implements StorageService {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(@org.springframework.beans.factory.annotation.Autowired(required = false) Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        if (cloudName == null || cloudName.isEmpty() || cloudinary == null) {
            log.warn("Cloudinary not configured. Returning fallback mock URL.");
            return "https://res.cloudinary.com/mock/image/upload/v123456/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        }
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String url = (String) uploadResult.get("secure_url");
            log.info("Uploaded file to Cloudinary: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage());
        }
    }
}
