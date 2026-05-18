package com.shigui.dto;

import lombok.Data;

/** 文件上传响应——返回可公开访问的图片 URL */
@Data
public class FileUploadResponse {
    private String url;
}
