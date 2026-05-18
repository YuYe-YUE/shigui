package com.shigui.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** AI 认领预审配置，读取 ai.claim.* 前缀的 application 属性 */
@Data
@Component
public class AiClaimReviewProperties {
    @Value("${ai.claim.enabled:true}")
    private boolean enabled;
    @Value("${ai.claim.base-url:}")
    private String baseUrl;
    @Value("${ai.claim.api-key:}")
    private String apiKey;
    @Value("${ai.claim.model:}")
    private String model;
    @Value("${ai.claim.timeout-seconds:30}")
    private int timeoutSeconds;
    @Value("${ai.claim.auto-approve-threshold:0.85}")
    private String autoApproveThreshold;
    @Value("${ai.claim.auto-reject-threshold:0.85}")
    private String autoRejectThreshold;

    public BigDecimal autoApproveThresholdValue() {
        return new BigDecimal(autoApproveThreshold);
    }

    public BigDecimal autoRejectThresholdValue() {
        return new BigDecimal(autoRejectThreshold);
    }
}
