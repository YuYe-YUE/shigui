package com.shigui.service;

import com.shigui.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String DEFAULT_UPLOAD_DIR = "uploads";
    String DEFAULT_PUBLIC_PREFIX = "/uploads";

    FileUploadResponse storePostImage(MultipartFile file);
}
