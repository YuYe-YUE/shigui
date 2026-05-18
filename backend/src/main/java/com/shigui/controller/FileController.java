package com.shigui.controller;

import com.shigui.common.Result;
import com.shigui.dto.FileUploadResponse;
import com.shigui.service.FileStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口，用于帖子图片等资源的上传。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // 上传图片文件，返回访问 URL
    @PostMapping("/upload")
    public Result<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.storePostImage(file));
    }
}
