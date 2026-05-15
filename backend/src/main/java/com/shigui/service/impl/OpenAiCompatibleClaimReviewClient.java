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

import java.time.Duration;
import java.util.List;
import java.util.Map;

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

        String response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                .body(body)
                .retrieve()
                .body(String.class);
        return parseResponse(response);
    }

    private String systemPrompt() {
        return """
                你是校园失物招领系统的 AI 管理员，负责判断认领答案是否能证明申请人是物主。
                只返回 JSON: {"decision":"APPROVE|REJECT|NEEDS_REVIEW","confidence":0.0,"reason":"简短中文原因"}。
                confidence 必须在 0 到 1 之间。
                如果答案和私密特征高度一致，decision=APPROVE。
                如果答案明显矛盾，decision=REJECT。
                如果答案含糊、信息不足或可能误伤，decision=NEEDS_REVIEW。
                reason 不得复述私密特征原文、卡号、学号、编号或连续数字，只能使用“私密特征匹配/不匹配/信息不足”等概括表达。\
                """;
    }

    private String userPrompt(LostFoundPost post, String answer) {
        return """
                FOUND_POST:
                id=%s
                title=%s
                itemName=%s
                itemCategory=%s
                description=%s
                privateFeature=%s
                campusArea=%s
                locationName=%s
                eventTime=%s

                CLAIM_ANSWER:
                %s
                """.formatted(post.getId(), post.getTitle(), post.getItemName(), post.getItemCategory(),
                post.getDescription(), post.getPrivateFeature(), post.getCampusArea(), post.getLocationName(),
                post.getEventTime(), answer);
    }

    private AiClaimReviewResult parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String content = root.at("/choices/0/message/content").asText();
            String json = content.replace("```json", "").replace("```", "").trim();
            AiClaimReviewResult result = objectMapper.readValue(json, AiClaimReviewResult.class);
            if (!List.of("APPROVE", "REJECT", "NEEDS_REVIEW").contains(result.getDecision())) {
                throw new IllegalStateException("Unknown claim review decision: " + result.getDecision());
            }
            result.setReason(sanitizeReason(result.getDecision()));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI claim review response: " + e.getMessage(), e);
        }
    }

    private String sanitizeReason(String decision) {
        return switch (decision) {
            case "APPROVE" -> "私密特征匹配";
            case "REJECT" -> "私密特征不匹配";
            case "NEEDS_REVIEW" -> "信息不足，需人工复核";
            default -> "需人工复核";
        };
    }

    private void validateConfig() {
        if (!properties.isEnabled()) throw new IllegalStateException("AI claim review is disabled");
        if (isBlank(properties.getBaseUrl())) throw new IllegalStateException("AI_CLAIM_BASE_URL is required");
        if (isBlank(properties.getApiKey())) throw new IllegalStateException("AI_CLAIM_API_KEY is required");
        if (isBlank(properties.getModel())) throw new IllegalStateException("AI_CLAIM_MODEL is required");
    }

    private String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
