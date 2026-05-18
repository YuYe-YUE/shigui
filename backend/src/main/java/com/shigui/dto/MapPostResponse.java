package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 地图点位响应——仅含展示必需字段，不含私密信息 */
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
