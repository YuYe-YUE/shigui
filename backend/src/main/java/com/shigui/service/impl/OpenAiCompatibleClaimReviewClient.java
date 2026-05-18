package com.shigui.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.AiClaimReviewResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.AiClaimReviewClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class OpenAiCompatibleClaimReviewClient implements AiClaimReviewClient {
    private final AiClaimReviewProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleClaimReviewClient(AiClaimReviewProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiClaimReviewResult reviewClaim(LostFoundPost foundPost, String privateFeatureAnswer) {
        validateConfig();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        RestClient client = RestClient.builder()
                .baseUrl(stripTrailingSlash(properties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", userPrompt(foundPost, privateFeatureAnswer))
                )
        );

        String response = client.post().uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                .body(body).retrieve().body(String.class);
        return parseResponse(response);
    }

    private String systemPrompt() {
        return "你是校园失物招领 AI 管理员。判断认领答案是否能证明申请人是物主。只返回 JSON: {\"decision\":\"APPROVE|REJECT|NEEDS_REVIEW\",\"confidence\":0.0,\"reason\":\"简短中文原因\"}。答案与私密特征高度一致→APPROVE，明显矛盾→REJECT，含糊或信息不足→NEEDS_REVIEW。reason 不得复述私密特征原文、卡号、学号、编号或连续数字。";
    }

    private String userPrompt(LostFoundPost post, String answer) {
        return String.format("FOUND_POST: id=%d title=%s itemName=%s itemCategory=%s description=%s privateFeature=%s campusArea=%s locationName=%s eventTime=%s\nCLAIM_ANSWER: %s",
                post.getId(), post.getTitle(), post.getItemName(), post.getItemCategory(),
                post.getDescription(), post.getPrivateFeature(), post.getCampusArea(),
                post.getLocationName(), post.getEventTime(), answer);
    }

    private AiClaimReviewResult parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String content = root.at("/choices/0/message/content").asText();
            String json = content.replace("```json", "").replace("```", "").trim();
            AiClaimReviewResult result = objectMapper.readValue(json, AiClaimReviewResult.class);
            if (!List.of("APPROVE", "REJECT", "NEEDS_REVIEW").contains(result.getDecision())) {
                throw new IllegalStateException("Unknown decision: " + result.getDecision());
            }
            result.setConfidence(normalize(result.getConfidence()));
            result.setReason(sanitizeReason(result.getReason()));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI claim review: " + e.getMessage(), e);
        }
    }

    private BigDecimal normalize(BigDecimal confidence) {
        if (confidence == null) return BigDecimal.ZERO;
        return confidence.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) return "AI 已完成判断";
        String sanitized = Pattern.compile("[A-Za-z0-9]{3,}").matcher(reason).replaceAll("***");
        sanitized = Pattern.compile("[一二三四五六七八九零〇]{4,}").matcher(sanitized).replaceAll("***");
        return sanitized.trim();
    }

    private void validateConfig() {
        if (!properties.isEnabled()) throw new IllegalStateException("AI claim review disabled");
        if (isBlank(properties.getBaseUrl())) throw new IllegalStateException("base URL required");
        if (isBlank(properties.getApiKey())) throw new IllegalStateException("API key required");
        if (isBlank(properties.getModel())) throw new IllegalStateException("model required");
    }

    private String stripTrailingSlash(String url) { return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url; }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
