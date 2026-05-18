package com.shigui.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** 用户端认领记录响应——含单据摘要与认领状态 */
@Data
public class ClaimResponse {
    private Long id; private Long postId; private String postTitle;
    private String itemName; private String itemCategory;
    private String campusArea; private String locationName;
    private String verifiedStorageLocation; private String status;
    private String aiDecision; private BigDecimal aiConfidence;
    private String aiReason; private String adminReason;
    private LocalDateTime createdAt; private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
}
