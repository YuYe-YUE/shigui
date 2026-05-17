package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MapPostResponse {
    private Long id;
    private String itemName;
    private String itemCategory;
    private String campusArea;
    private String locationName;
    private Double longitude;
    private Double latitude;
    private LocalDateTime eventTime;
}
