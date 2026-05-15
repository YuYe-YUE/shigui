package com.shigui.service;
import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.AiClaimReviewResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.impl.OpenAiCompatibleClaimReviewClient;
import org.junit.jupiter.api.Test;
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
    void reviewClaim_realApi_approvesStrongPrivateFeatureMatch() {
        String baseUrl = value("AI_CLAIM_BASE_URL");
        String apiKey = value("AI_CLAIM_API_KEY");
        String model = value("AI_CLAIM_MODEL");
        if (baseUrl == null || apiKey == null || model == null)
            fail("AI_CLAIM_BASE_URL, AI_CLAIM_API_KEY, AI_CLAIM_MODEL must be set");

        AiClaimReviewProperties p = new AiClaimReviewProperties();
        p.setEnabled(true); p.setBaseUrl(baseUrl); p.setApiKey(apiKey); p.setModel(model);
        p.setTimeoutSeconds(30); p.setAutoApproveThreshold("0.85"); p.setAutoRejectThreshold("0.85");

        LostFoundPost post = new LostFoundPost(); post.setId(1L); post.setPostType("FOUND");
        post.setTitle("捡到校园卡"); post.setItemName("校园卡"); post.setItemCategory("证件");
        post.setDescription("绿色卡套"); post.setPrivateFeature("后四位9876");
        post.setCampusArea("南校园"); post.setLocationName("逸夫楼");
        post.setEventTime(LocalDateTime.of(2026,5,15,12,0));

        AiClaimReviewResult r = new OpenAiCompatibleClaimReviewClient(p).reviewClaim(post, "后四位9876");
        assertThat(r.getDecision()).isIn("APPROVE","NEEDS_REVIEW");
        assertThat(r.getConfidence()).isNotNull();
        assertThat(r.getReason()).doesNotContain("9876");
    }
}
