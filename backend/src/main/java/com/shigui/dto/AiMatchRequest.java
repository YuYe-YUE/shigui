package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
