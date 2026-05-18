package com.shigui.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.*;
import com.shigui.entity.ChatSession;
import java.util.List;

public interface ChatService extends IService<ChatSession> {
    ChatSessionResponse createOrGetSession(Long userId, Long postId);
    List<ChatMessageResponse> listMessages(Long userId, Long sessionId);
    ChatMessageResponse sendMessage(Long userId, Long sessionId, String content);
}
