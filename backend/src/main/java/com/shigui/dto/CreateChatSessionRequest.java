package com.shigui.dto;

import lombok.Data;

/** 创建聊天会话请求——指明会话关联的单据 */
@Data
public class CreateChatSessionRequest {
    private Long postId;
}
