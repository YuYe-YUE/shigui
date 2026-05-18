package com.shigui.dto;

import lombok.Data;

/** 发送聊天消息请求——仅含消息文本内容 */
@Data
public class SendMessageRequest {
    private String content;
}
