package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 用户端匹配结果响应——含我的单据和匹配到的对方单据 */
@Data
public class MatchResponse {
    private Long id;
    private BigDecimal score;
    private String reason;
    private PostResponse myPost;
    private PostResponse matchedPost;
    private LocalDateTime createdAt;
}
