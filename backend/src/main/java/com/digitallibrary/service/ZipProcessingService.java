package com.digitallibrary.service;

import com.digitallibrary.dto.ZipContents;
import org.springframework.web.multipart.MultipartFile;

public interface ZipProcessingService {
    ZipContents processZipFile(MultipartFile zipFile);
}
