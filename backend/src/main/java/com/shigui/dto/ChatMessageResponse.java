package com.shigui.dto;
import lombok.Data;
import java.time.LocalDateTime;
/** 聊天消息响应——含发送方角色及是否为自己发送 */
@Data
public class ChatMessageResponse {
    private Long id; private Long sessionId;
    private String senderRole; private Boolean mine; private String content;
    private String msgType; private LocalDateTime createdAt;
}
