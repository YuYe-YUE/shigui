package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理端匹配结果列表响应——展示失物与招领双方单据摘要 */
@Data
public class AdminMatchResponse {
    private Long id;
    private Long lostPostId;
    private String lostTitle;
    private String lostItemName;
    private String lostCampusArea;
    private Long foundPostId;
    private String foundTitle;
    private String foundItemName;
    private String foundCampusArea;
    private BigDecimal score;
    private String reason;
    private LocalDateTime createdAt;
}
