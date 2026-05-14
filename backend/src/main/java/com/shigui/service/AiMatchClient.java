package com.shigui.service;

import com.shigui.dto.AiMatchResult;
import com.shigui.entity.LostFoundPost;

import java.util.List;

public interface AiMatchClient {
    AiMatchResult rankMatches(LostFoundPost targetPost, List<LostFoundPost> candidates);
}
