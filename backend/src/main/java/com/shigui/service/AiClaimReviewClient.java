package com.shigui.service;

import com.shigui.dto.AiClaimReviewResult;
import com.shigui.entity.LostFoundPost;

/**
 * AI 认领审查客户端：调用大模型判断认领答案是否匹配私密特征。
 */
public interface AiClaimReviewClient {
    /** 提交认领预审，返回 AI 的决策、置信度和原因 */
    AiClaimReviewResult reviewClaim(LostFoundPost foundPost, String privateFeatureAnswer);
}
