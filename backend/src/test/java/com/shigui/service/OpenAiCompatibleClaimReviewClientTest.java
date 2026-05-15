package com.shigui.service;

import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.AiClaimReviewResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.impl.OpenAiCompatibleClaimReviewClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class OpenAiCompatibleClaimReviewClientTest {
    private static String value(String name) {
        String v = System.getProperty(name);
        if (v != null && !v.isBlank()) return v;
        v = System.getenv(name);
        return (v != null && !v.isBlank()) ? v : null;
    }

    @Test
    void reviewClaim_realApi_approvesStrongPrivateFeatureMatchWithoutRepeatingLongPrivateNumbers() {
        String baseUrl = value("AI_CLAIM_BASE_URL");
        String apiKey = value("AI_CLAIM_API_KEY");
        String model = value("AI_CLAIM_MODEL");
        if (baseUrl == null || apiKey == null || model == null) {
            fail("AI_CLAIM_BASE_URL, AI_CLAIM_API_KEY, AI_CLAIM_MODEL must be set via -D or env");
        }

        AiClaimReviewProperties properties = new AiClaimReviewProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(baseUrl);
        properties.setApiKey(apiKey);
        properties.setModel(model);
        properties.setTimeoutSeconds(30);
        properties.setAutoApproveThreshold("0.85");
        properties.setAutoRejectThreshold("0.85");

        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        post.setPostType("FOUND");
        post.setTitle("捡到绿色卡套校园卡");
        post.setItemName("校园卡");
        post.setItemCategory("证件");
        post.setDescription("在南校园逸夫楼门口捡到一张校园卡，绿色透明卡套");
        post.setPrivateFeature("学号2234567890，卡套里有蓝色星星贴纸");
        post.setCampusArea("南校园");
        post.setLocationName("逸夫楼");
        post.setEventTime(LocalDateTime.of(2026, 5, 15, 12, 0));

        OpenAiCompatibleClaimReviewClient client = new OpenAiCompatibleClaimReviewClient(properties);
        AiClaimReviewResult result = client.reviewClaim(post, "学号2234567890，卡套里有蓝色星星贴纸");

        assertThat(result.getDecision()).isIn("APPROVE", "NEEDS_REVIEW");
        assertThat(result.getConfidence()).isNotNull();
        assertThat(result.getReason()).isNotBlank();
        assertThat(result.getReason()).doesNotContain("2234567890");
        assertThat(result.getReason()).doesNotContainPattern("\\d{4,}");
    }

    @Test
    void reviewClaim_sanitizesLongNumbersReturnedInReason() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            String response = """
                    {"choices":[{"message":{"content":"{\\"decision\\":\\"APPROVE\\",\\"confidence\\":0.91,\\"reason\\":\\"学号2234567890匹配\\"}"}}]}
                    """;
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            AiClaimReviewProperties properties = new AiClaimReviewProperties();
            properties.setEnabled(true);
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            properties.setApiKey("dummy");
            properties.setModel("dummy");
            properties.setTimeoutSeconds(5);

            OpenAiCompatibleClaimReviewClient client = new OpenAiCompatibleClaimReviewClient(properties);
            AiClaimReviewResult result = client.reviewClaim(foundPost(), "学号2234567890，卡套里有蓝色星星贴纸");

            assertThat(result.getDecision()).isEqualTo("APPROVE");
            assertThat(result.getReason()).doesNotContain("2234567890");
            assertThat(result.getReason()).doesNotContainPattern("\\d{4,}");
        } finally {
            server.stop(0);
        }
    }

    private static LostFoundPost foundPost() {
        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        post.setPostType("FOUND");
        post.setTitle("捡到绿色卡套校园卡");
        post.setItemName("校园卡");
        post.setItemCategory("证件");
        post.setDescription("在南校园逸夫楼门口捡到一张校园卡，绿色透明卡套");
        post.setPrivateFeature("学号2234567890，卡套里有蓝色星星贴纸");
        post.setCampusArea("南校园");
        post.setLocationName("逸夫楼");
        post.setEventTime(LocalDateTime.of(2026, 5, 15, 12, 0));
        return post;
    }
}
