package com.shigui.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shigui.config.AiMatchProperties;
import com.shigui.dto.AiMatchResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.AiMatchClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Service
public class OpenAiCompatibleMatchClient implements AiMatchClient {
    private final AiMatchProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleMatchClient(AiMatchProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiMatchResult rankMatches(LostFoundPost targetPost, List<LostFoundPost> candidates) {
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
                        Map.of("role", "user", "content", userPrompt(targetPost, candidates))
                )
        );

        String response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                .body(body)
                .retrieve()
                .body(String.class);
        return parseResponse(response, candidates);
    }

    private void validateConfig() {
        if (!properties.isEnabled()) throw new IllegalStateException("AI matching is disabled");
        if (isBlank(properties.getBaseUrl())) throw new IllegalStateException("AI_MATCH_BASE_URL is required");
        if (isBlank(properties.getApiKey())) throw new IllegalStateException("AI_MATCH_API_KEY is required");
        if (isBlank(properties.getModel())) throw new IllegalStateException("AI_MATCH_MODEL is required");
    }

    private String systemPrompt() {
        return """
                You are an expert lost-and-found matching engine.
                Your job: for each CANDIDATE, output matched=true if it plausibly refers to the SAME item described in TARGET.
                Consider: item category, title similarity, description keywords, privateFeature keywords (if provided), campus, location closeness, time difference.
                IMPORTANT: Never repeat or quote privateFeature text in the reason. Use generic phrases like "私密特征匹配" instead.
                For matched=true candidates, assign a score 0.0-1.0 and a short reason in Chinese (max 50 chars).
                For matched=false, set score=0.0 and reason="".
                Return ONLY valid JSON matching the requested format.\
                """;
    }

    private String userPrompt(LostFoundPost target, List<LostFoundPost> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("TARGET:\n").append(formatPost(target)).append("\n\nCANDIDATES:\n");
        for (int i = 0; i < candidates.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(formatPost(candidates.get(i))).append("\n");
        }
        sb.append("\nReturn JSON: {\"matches\":[{\"candidatePostId\":<id>,\"matched\":true|false,\"score\":0.0-1.0,\"reason\":\"\"}]}");
        return sb.toString();
    }

    private String formatPost(LostFoundPost p) {
        StringBuilder s = new StringBuilder();
        s.append("id=").append(p.getId());
        s.append(", postType=").append(p.getPostType());
        s.append(", title=").append(p.getTitle());
        s.append(", itemName=").append(p.getItemName());
        s.append(", itemCategory=").append(p.getItemCategory());
        s.append(", description=").append(p.getDescription());
        if (properties.isIncludePrivateFeature()) {
            s.append(", privateFeature=").append(p.getPrivateFeature());
        }
        s.append(", campusArea=").append(p.getCampusArea());
        s.append(", locationName=").append(p.getLocationName());
        s.append(", eventTime=").append(p.getEventTime());
        return s.toString();
    }

    private AiMatchResult parseResponse(String body, List<LostFoundPost> candidates) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode content = root.at("/choices/0/message/content");
            String json = content.asText().replace("```json", "").replace("```", "").trim();
            AiMatchResult result = objectMapper.readValue(json, AiMatchResult.class);
            // Validate all candidate IDs reference actual candidates
            for (AiMatchResult.Decision d : result.getMatches()) {
                boolean exists = candidates.stream().anyMatch(c -> c.getId().equals(d.getCandidatePostId()));
                if (!exists) throw new IllegalStateException("AI returned unknown candidate ID: " + d.getCandidatePostId());
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    private String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
