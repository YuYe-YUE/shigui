package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private String coverImageUrl;
    private List<String> imageUrls;
}
