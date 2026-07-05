package com.renthub.storage.controller;

import com.renthub.common.dto.ApiResponse;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.storage.service.LocalStorageService;
import com.renthub.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "File Storage", description = "Endpoints for uploading images and booking documents.")
public class StorageController {

    private final StorageService storageService;
    private final LocalStorageService localStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", description = "Uploads an image or document (<10MB) and returns its URL.")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = storageService.uploadFile(file);
        return ResponseEntity.ok(ApiResponse.success(fileUrl, "File uploaded successfully"));
    }

    @GetMapping("/files/{fileName:.+}")
    @Operation(summary = "Get local file", description = "Serves locally stored files (images, PDFs) by name.")
    public ResponseEntity<Resource> getLocalFile(@PathVariable String fileName) {
        try {
            Path filePath = localStorageService.getFileLocation(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new ResourceNotFoundException("File", "name", fileName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File", "name", fileName);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
