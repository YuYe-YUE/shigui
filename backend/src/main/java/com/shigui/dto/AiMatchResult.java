package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** AI 匹配接口响应体——每个候选的匹配决策列表 */
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
