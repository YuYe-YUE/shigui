package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 发布单据请求——失物/招领共用，图片通过 URL 列表传入 */
@Data
public class CreatePostRequest {
    private String postType;
    private String title;
    private String itemName;
    private String itemCategory;
    private String description;
    private String privateFeature;
    private String campusArea;
    private String locationName;
    private Double longitude;
    private Double latitude;
    private String storageLocation;
    private LocalDateTime eventTime;
    private List<String> imageUrls;
}
