package com.shigui.service;

import com.shigui.dto.AiClaimReviewResult;
import com.shigui.entity.LostFoundPost;

public interface AiClaimReviewClient {
    AiClaimReviewResult reviewClaim(LostFoundPost foundPost, String privateFeatureAnswer);
}
