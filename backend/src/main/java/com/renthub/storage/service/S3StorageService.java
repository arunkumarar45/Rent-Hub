package com.renthub.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service("s3Storage")
@Slf4j
public class S3StorageService implements StorageService {

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    private final S3Client s3Client;

    public S3StorageService(@org.springframework.beans.factory.annotation.Autowired(required = false) S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        if (bucketName == null || bucketName.isEmpty() || s3Client == null) {
            log.warn("S3 bucket or client not configured. Returning fallback mock URL.");
            return "https://renthub-mock-s3-bucket.s3.amazonaws.com/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        }
        try {
            String key = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Uploaded file to S3: {}", key);
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
        } catch (Exception e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("S3 upload failed: " + e.getMessage());
        }
    }
}
