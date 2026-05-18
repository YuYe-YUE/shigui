package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** AI 匹配接口请求体——目标单据 + 候选单据列表 */
@Data
public class AiMatchRequest {
    private Candidate target;
    private List<Candidate> candidates;

    @Data
    public static class Candidate {
        private Long id;
        private String postType;
        private String title;
        private String itemName;
        private String itemCategory;
        private String description;
        private String privateFeature;
        private String campusArea;
        private String locationName;
        private LocalDateTime eventTime;
    }
}
