package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理端认领审核详情响应——包含单据、用户及 AI 预审全部信息 */
@Data
public class AdminClaimResponse {
    private Long id;
    private Long postId;
    private String postTitle;
    private String itemName;
    private String itemCategory;
    private String campusArea;
    private String locationName;
    private String storageLocation;
    private String privateFeature;
    private Long claimantUserId;
    private String privateFeatureAnswer;
    private String status;
    private String aiDecision;
    private BigDecimal aiConfidence;
    private String aiReason;
    private String adminReason;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
}
