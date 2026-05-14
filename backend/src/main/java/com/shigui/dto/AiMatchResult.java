package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiMatchResult {
    private List<Decision> matches = new ArrayList<>();

    @Data
    public static class Decision {
        private Long candidatePostId;
        private Boolean matched;
        private BigDecimal score;
        private String reason;
    }
}
