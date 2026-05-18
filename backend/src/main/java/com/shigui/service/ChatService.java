package com.shigui.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.*;
import com.shigui.entity.ChatSession;
import java.util.List;

/**
 * 聊天服务：失主与拾捡者之间的会话管理和消息收发。
 */
public interface ChatService extends IService<ChatSession> {
    /** 创建或获取已有会话，禁止与自己或已关闭的单据聊天 */
    ChatSessionResponse createOrGetSession(Long userId, Long postId);
    /** 获取会话中的消息列表（按时间正序） */
    List<ChatMessageResponse> listMessages(Long userId, Long sessionId);
    /** 发送文本消息，封禁用户不可发送 */
    ChatMessageResponse sendMessage(Long userId, Long sessionId, String content);
}
