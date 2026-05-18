package com.shigui.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class ChatMessageResponse {
    private Long id; private Long sessionId;
    private String senderRole; private Boolean mine; private String content;
    private String msgType; private LocalDateTime createdAt;
}
