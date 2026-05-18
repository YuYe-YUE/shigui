package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;

/** AI 认领预审返回结果：决策 + 置信度 + 理由 */
@Data
public class AiClaimReviewResult {
    private String decision;
    private BigDecimal confidence;
    private String reason;
}
