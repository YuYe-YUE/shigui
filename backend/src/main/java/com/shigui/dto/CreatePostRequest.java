package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

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
}
