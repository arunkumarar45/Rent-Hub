package com.renthub.storage.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
public class DelegatingStorageService implements StorageService {

    private final StorageService localStorageService;
    private final StorageService s3StorageService;
    private final StorageService cloudinaryStorageService;

    @Value("${storage.provider:local}")
    private String storageProvider;

    public DelegatingStorageService(
            @Qualifier("localStorage") StorageService localStorageService,
            @Qualifier("s3Storage") StorageService s3StorageService,
            @Qualifier("cloudinaryStorage") StorageService cloudinaryStorageService) {
        this.localStorageService = localStorageService;
        this.s3StorageService = s3StorageService;
        this.cloudinaryStorageService = cloudinaryStorageService;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        switch (storageProvider.toLowerCase()) {
            case "s3":
                return s3StorageService.uploadFile(file);
            case "cloudinary":
                return cloudinaryStorageService.uploadFile(file);
            case "local":
            default:
                return localStorageService.uploadFile(file);
        }
    }
}
