package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MatchResponse {
    private Long id;
    private BigDecimal score;
    private String reason;
    private PostResponse myPost;
    private PostResponse matchedPost;
    private LocalDateTime createdAt;
}
