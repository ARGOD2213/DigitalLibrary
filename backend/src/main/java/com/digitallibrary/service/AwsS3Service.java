package com.digitallibrary.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;

public interface AwsS3Service {
    String uploadFile(MultipartFile file, String folderKey);
    String uploadFile(File file, String s3Key, String contentType);
    void deleteFile(String s3Key);
    String generatePresignedUrl(String s3Key, long expirationMinutes);
}
