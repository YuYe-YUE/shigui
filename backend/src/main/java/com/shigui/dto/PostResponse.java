package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResponse {
    private Long id;
    private String postType;
    private String title;
    private String itemName;
    private String itemCategory;
    private String description;
    private String campusArea;
    private String locationName;
    private String storageLocation;
    private LocalDateTime eventTime;
    private LocalDateTime publishedAt;
    private String status;
}
