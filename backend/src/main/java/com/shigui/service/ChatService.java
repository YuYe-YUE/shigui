package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.ChatMessageResponse;
import com.shigui.dto.ChatSessionResponse;
import com.shigui.entity.ChatSession;

import java.util.List;

public interface ChatService extends IService<ChatSession> {
    ChatSessionResponse createOrGetSession(Long userId, Long postId);

    List<ChatMessageResponse> listMessages(Long userId, Long sessionId);

    ChatMessageResponse sendMessage(Long userId, Long sessionId, String content);
}
