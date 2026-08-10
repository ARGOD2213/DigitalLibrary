package com.digitallibrary.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.digitallibrary.dto.ZipContents;
import com.digitallibrary.service.AwsS3Service;
import com.digitallibrary.service.ZipProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipProcessingServiceImpl implements ZipProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ZipProcessingServiceImpl.class);
    private static final long PRESIGNED_EXPIRATION_MINUTES = 2880; // 48 hours

    private final AwsS3Service awsS3Service;
    private final ObjectMapper objectMapper;

    public ZipProcessingServiceImpl(AwsS3Service awsS3Service, ObjectMapper objectMapper) {
        this.awsS3Service = awsS3Service;
        this.objectMapper = objectMapper;
    }

    @Override
    public ZipContents processZipFile(MultipartFile zipFile) {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("ZIP file is empty or missing");
        }

        String originalName = zipFile.getOriginalFilename();
        if (originalName != null && !originalName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Uploaded file is not a valid .zip file");
        }

        ZipContents contents = new ZipContents();
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("zip_extract_" + UUID.randomUUID());
            int fileCount = 0;

            try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        zis.closeEntry();
                        continue;
                    }

                    String entryName = entry.getName();
                    // Prevent Zip Slip vulnerability
                    Path targetPath = tempDir.resolve(entryName).normalize();
                    if (!targetPath.startsWith(tempDir)) {
                        throw new SecurityException("Zip entry tries to break out of target directory: " + entryName);
                    }

                    Files.createDirectories(targetPath.getParent());
                    try (OutputStream os = Files.newOutputStream(targetPath)) {
                        zis.transferTo(os);
                    }

                    File extractedFile = targetPath.toFile();
                    fileCount++;

                    processExtractedFile(extractedFile, entryName, contents);
                    zis.closeEntry();
                }
            }

            contents.setExtractedFilesCount(fileCount);
            log.info("Successfully processed ZIP file {} with {} files extracted", originalName, fileCount);
            return contents;

        } catch (IOException ex) {
            log.error("Failed to extract and process ZIP file {}", originalName, ex);
            throw new IllegalStateException("Failed to process ZIP archive: " + ex.getMessage(), ex);
        } finally {
            if (tempDir != null) {
                deleteDirectoryRecursively(tempDir.toFile());
            }
        }
    }

    private void processExtractedFile(File file, String entryName, ZipContents contents) {
        String lowerName = entryName.toLowerCase();

        if (lowerName.endsWith("metadata.json")) {
            try {
                Map<String, Object> metaMap = objectMapper.readValue(file, new TypeReference<HashMap<String, Object>>() {});
                contents.setMetadata(metaMap);
                log.info("Extracted metadata.json from ZIP: {}", metaMap.keySet());
            } catch (Exception e) {
                log.warn("Failed to parse metadata.json in ZIP entry {}", entryName, e);
            }
        } else if (isImageFile(lowerName)) {
            String contentType = getContentType(lowerName);
            String s3Key = "covers/" + UUID.randomUUID() + "-" + file.getName();
            String uploadedKey = awsS3Service.uploadFile(file, s3Key, contentType);
            String presignedUrl = awsS3Service.generatePresignedUrl(uploadedKey, PRESIGNED_EXPIRATION_MINUTES);

            contents.setCoverFileKey(uploadedKey);
            contents.setCoverFileUrl(presignedUrl);
            log.info("Extracted and uploaded cover image {}", uploadedKey);
        } else if (isDocumentFile(lowerName)) {
            String contentType = getContentType(lowerName);
            String s3Key = "books/" + UUID.randomUUID() + "-" + file.getName();
            String uploadedKey = awsS3Service.uploadFile(file, s3Key, contentType);
            String presignedUrl = awsS3Service.generatePresignedUrl(uploadedKey, PRESIGNED_EXPIRATION_MINUTES);

            contents.setBookFileKey(uploadedKey);
            contents.setBookFileUrl(presignedUrl);
            log.info("Extracted and uploaded book document {}", uploadedKey);
        }
    }

    private boolean isImageFile(String name) {
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }

    private boolean isDocumentFile(String name) {
        return name.endsWith(".pdf") || name.endsWith(".epub") || name.endsWith(".mobi") || name.endsWith(".txt");
    }

    private String getContentType(String name) {
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".epub")) return "application/epub+zip";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private void deleteDirectoryRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteDirectoryRecursively(child);
            }
        }
        file.delete();
    }
}
