package com.shigui.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.*;
import com.shigui.entity.ChatMessage;
import java.util.List;

public interface ChatService extends IService<ChatMessage> {
    ChatSessionResponse createSession(Long userId, CreateChatSessionRequest request);
    List<ChatMessageResponse> getMessages(Long sessionId, Long userId);
    ChatMessageResponse sendMessage(Long sessionId, Long userId, SendMessageRequest request);
}
