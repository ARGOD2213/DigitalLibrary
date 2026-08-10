package com.digitallibrary.service.impl;

import com.digitallibrary.service.AwsS3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
public class AwsS3ServiceImpl implements AwsS3Service {

    private static final Logger log = LoggerFactory.getLogger(AwsS3ServiceImpl.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name:digital-library-storage}")
    private String bucketName;

    @Value("${aws.mock-enabled:true}")
    private boolean mockEnabled;

    public AwsS3ServiceImpl(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public String uploadFile(MultipartFile file, String folderKey) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String s3Key = (folderKey != null && !folderKey.isBlank() ? folderKey.replaceAll("^/|/$", "") + "/" : "")
                + UUID.randomUUID() + extension;

        if (mockEnabled) {
            log.info("[MOCK S3] Uploaded file {} ({} bytes) to key {}", originalFilename, file.getSize(), s3Key);
            return s3Key;
        }

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
            log.info("Successfully uploaded file {} to S3 bucket {} key {}", originalFilename, bucketName, s3Key);
            return s3Key;
        } catch (Exception e) {
            log.error("Failed to upload file {} to S3 bucket {}", originalFilename, bucketName, e);
            throw new RuntimeException("S3 Upload Failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadFile(File file, String s3Key, String contentType) {
        if (mockEnabled) {
            log.info("[MOCK S3] Uploaded File object {} to key {}", file.getName(), s3Key);
            return s3Key;
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            log.info("Successfully uploaded File {} to S3 bucket {} key {}", file.getName(), bucketName, s3Key);
            return s3Key;
        } catch (Exception e) {
            log.error("Failed to upload File {} to S3 bucket {}", file.getName(), bucketName, e);
            throw new RuntimeException("S3 File Upload Failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) return;

        if (mockEnabled) {
            log.info("[MOCK S3] Deleted object key {}", s3Key);
            return;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Deleted S3 object key {} from bucket {}", s3Key, bucketName);
        } catch (Exception e) {
            log.error("Failed to delete S3 object key {} from bucket {}", s3Key, bucketName, e);
        }
    }

    @Override
    public String generatePresignedUrl(String s3Key, long expirationMinutes) {
        if (s3Key == null || s3Key.isBlank()) return null;

        if (mockEnabled) {
            log.info("[MOCK S3] Generated pre-signed URL for key {}", s3Key);
            return "http://localhost:8000/api/files/download?key=" + s3Key;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();
            log.info("Generated S3 Pre-signed URL for key {} (expires in {} mins)", s3Key, expirationMinutes);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL for S3 key {}", s3Key, e);
            return "http://localhost:8000/api/files/download?key=" + s3Key;
        }
    }
}
