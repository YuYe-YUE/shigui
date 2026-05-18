package com.shigui.service;

import com.shigui.dto.AiMatchResult;
import com.shigui.entity.LostFoundPost;

import java.util.List;

/**
 * AI 匹配客户端：调用大模型对目标单据和候选单据进行智能匹配排序。
 */
public interface AiMatchClient {
    /** 对候选列表进行 AI 排序评分，返回匹配结果及置信度 */
    AiMatchResult rankMatches(LostFoundPost targetPost, List<LostFoundPost> candidates);
}
