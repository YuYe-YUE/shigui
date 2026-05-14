package com.shigui.service;

import com.shigui.config.AiMatchProperties;
import com.shigui.dto.AiMatchResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.impl.OpenAiCompatibleMatchClient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleMatchClientTest {

    @Test
    void rankMatches_realApi_returnsStrongMatchAndRejectsNoise() {
        String baseUrl = System.getenv("AI_MATCH_BASE_URL");
        String apiKey = System.getenv("AI_MATCH_API_KEY");
        String model = System.getenv("AI_MATCH_MODEL");
        Assumptions.assumeTrue(baseUrl != null && !baseUrl.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank(),
                "Skipped: set AI_MATCH_BASE_URL, AI_MATCH_API_KEY, AI_MATCH_MODEL to run");

        AiMatchProperties properties = new AiMatchProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(baseUrl);
        properties.setApiKey(apiKey);
        properties.setModel(model);
        properties.setTimeoutSeconds(30);
        properties.setIncludePrivateFeature(true);
        properties.setMaxCandidates(20);
        properties.setMaxResults(5);
        properties.setThreshold("0.70");

        OpenAiCompatibleMatchClient client = new OpenAiCompatibleMatchClient(properties);
        LostFoundPost target = post(100L, 21L, "FOUND", "捡到绿色卡套校园卡", "校园卡", "证件",
                "在南校园逸夫楼门口捡到一张校园卡，绿色透明卡套", "卡号后四位1234，卡套里有蓝色贴纸",
                "南校园", "逸夫楼", LocalDateTime.of(2026, 5, 14, 12, 0));

        List<LostFoundPost> candidates = List.of(
                post(1L, 11L, "LOST", "丢失绿色卡套校园卡", "校园卡", "证件", "可能在逸夫楼附近丢失，绿色卡套", "后四位1234，蓝色贴纸", "南校园", "逸夫楼", LocalDateTime.of(2026, 5, 14, 9, 0)),
                post(2L, 12L, "LOST", "丢了校园卡", "校园卡", "证件", "在南校园教学楼附近丢失", "卡号不记得", "南校园", "第三教学楼", LocalDateTime.of(2026, 5, 13, 18, 0)),
                post(3L, 13L, "LOST", "黑色雨伞丢失", "雨伞", "生活用品", "黑色长柄伞", "伞柄有划痕", "南校园", "图书馆", LocalDateTime.of(2026, 5, 14, 10, 0))
        );

        AiMatchResult result = client.rankMatches(target, candidates);
        assertThat(result.getMatches()).isNotEmpty();
        AiMatchResult.Decision strong = result.getMatches().stream()
                .filter(item -> item.getCandidatePostId().equals(1L))
                .findFirst().orElseThrow();
        assertThat(strong.getMatched()).isTrue();
        assertThat(strong.getScore()).isGreaterThanOrEqualTo(new java.math.BigDecimal("0.70"));
        assertThat(strong.getReason()).isNotBlank();
        assertThat(strong.getReason()).doesNotContain("1234");
    }

    private static LostFoundPost post(Long id, Long userId, String postType, String title, String itemName,
                                      String itemCategory, String description, String privateFeature,
                                      String campusArea, String locationName, LocalDateTime eventTime) {
        LostFoundPost post = new LostFoundPost();
        post.setId(id); post.setUserId(userId); post.setPostType(postType);
        post.setTitle(title); post.setItemName(itemName); post.setItemCategory(itemCategory);
        post.setDescription(description); post.setPrivateFeature(privateFeature);
        post.setCampusArea(campusArea); post.setLocationName(locationName);
        post.setEventTime(eventTime); post.setStatus("MATCHING"); post.setDeleted(0);
        return post;
    }
}
