package com.shigui.service;

import com.shigui.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务：图片上传、存储路径校验的抽象接口。
 */
public interface FileStorageService {

    String DEFAULT_UPLOAD_DIR = "uploads";
    String DEFAULT_PUBLIC_PREFIX = "/uploads";

    /** 存储一张单据图片，返回可公开访问的 URL */
    FileUploadResponse storePostImage(MultipartFile file);

    /** 判断给定 URL 是否为本系统存储的有效图片 */
    boolean isStoredPostImage(String url);
}
