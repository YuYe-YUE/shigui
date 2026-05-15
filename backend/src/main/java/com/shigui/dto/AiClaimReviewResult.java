package com.shigui.dto;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class AiClaimReviewResult {
    private String decision;
    private BigDecimal confidence;
    private String reason;
}
