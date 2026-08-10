package com.digitallibrary.storage;

import com.digitallibrary.service.AwsS3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
public class S3FileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);
    private static final long DEFAULT_EXPIRATION_MINUTES = 2880; // 48 hours

    private final AwsS3Service awsS3Service;

    public S3FileStorageService(AwsS3Service awsS3Service) {
        this.awsS3Service = awsS3Service;
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required for storage");
        }

        String s3Key = awsS3Service.uploadFile(file, "books");
        String presignedUrl = awsS3Service.generatePresignedUrl(s3Key, DEFAULT_EXPIRATION_MINUTES);

        log.info("Stored file in S3 with key={} and generated pre-signed URL", s3Key);
        return new StoredFile(s3Key, presignedUrl);
    }
}
