package com.shigui.dto;
import lombok.Data;
/** 聊天会话响应——包含当前用户角色及对方角色 */
@Data
public class ChatSessionResponse {
    private Long id; private Long postId;
    private String currentUserRole; private String peerRole;
    private String status;
}
