package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ClaimResponse {
    private Long id;
    private Long postId;
    private String postTitle;
    private String itemName;
    private String itemCategory;
    private String campusArea;
    private String locationName;
    private String storageLocation;
    private String status;
    private String aiDecision;
    private BigDecimal aiConfidence;
    private String aiReason;
    private String adminReason;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
}
