package com.shigui.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** AI 智能匹配配置，读取 ai.match.* 前缀的 application 属性 */
@Data
@Component
public class AiMatchProperties {
    @Value("${ai.match.enabled:true}")
    private boolean enabled;
    @Value("${ai.match.base-url:}")
    private String baseUrl;
    @Value("${ai.match.api-key:}")
    private String apiKey;
    @Value("${ai.match.model:}")
    private String model;
    @Value("${ai.match.timeout-seconds:30}")
    private int timeoutSeconds;
    @Value("${ai.match.include-private-feature:true}")
    private boolean includePrivateFeature;
    @Value("${ai.match.max-candidates:20}")
    private int maxCandidates;
    @Value("${ai.match.max-results:5}")
    private int maxResults;
    @Value("${ai.match.threshold:0.70}")
    private String threshold;

    public BigDecimal thresholdValue() {
        return new BigDecimal(threshold);
    }
}
