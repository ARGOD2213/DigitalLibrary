package com.digitallibrary.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path storagePath;

    public LocalFileStorageService(@Value("${app.storage.local-folder}") String localFolder) {
        this.storagePath = Path.of(localFolder).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        try {
            Files.createDirectories(storagePath);
            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
            String fileName = UUID.randomUUID() + "-" + originalName;
            Path target = storagePath.resolve(fileName).normalize();
            file.transferTo(target);
            return new StoredFile(fileName, "/api/files/" + fileName);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store uploaded file", ex);
        }
    }

    public Path getStoragePath() {
        return storagePath;
    }
}
