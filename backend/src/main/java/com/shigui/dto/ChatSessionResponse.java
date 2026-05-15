package com.shigui.dto;
import lombok.Data;
@Data
public class ChatSessionResponse {
    private Long id; private Long postId;
    private Long lostUserId; private Long foundUserId;
    private String status;
}
