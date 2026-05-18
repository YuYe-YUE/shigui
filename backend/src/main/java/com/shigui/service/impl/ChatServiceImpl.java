package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.dto.ChatMessageResponse;
import com.shigui.dto.ChatSessionResponse;
import com.shigui.entity.AppUser;
import com.shigui.entity.ChatMessage;
import com.shigui.entity.ChatSession;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.ChatMessageMapper;
import com.shigui.mapper.ChatSessionMapper;
import com.shigui.service.AppUserService;
import com.shigui.service.ChatService;
import com.shigui.service.LostFoundPostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天实现：创建/复用会话、消息收发、参与者校验。
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatService {
    private static final String ACTIVE = "ACTIVE";
    private static final String LOST = "LOST";
    private static final String FOUND = "FOUND";

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final LostFoundPostService lostFoundPostService;
    private final AppUserService appUserService;

    public ChatServiceImpl(ChatSessionMapper chatSessionMapper,
                           ChatMessageMapper chatMessageMapper,
                           LostFoundPostService lostFoundPostService,
                           AppUserService appUserService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.lostFoundPostService = lostFoundPostService;
        this.appUserService = appUserService;
    }

    /** 创建或复用会话，校验单据状态和参与者合法性 */
    @Override
    @Transactional
    public ChatSessionResponse createOrGetSession(Long userId, Long postId) {
        AppUser user = appUserService.getByIdOrThrow(userId);
        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalArgumentException("封禁用户不能创建会话");
        }

        LostFoundPost post = lostFoundPostService.getById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())) {
            throw new IllegalArgumentException("单据不存在");
        }
        if (post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("不能和自己的单据创建会话");
        }
        if (!"MATCHING".equals(post.getStatus()) && !"CLAIMING".equals(post.getStatus()) && !"RETURNING".equals(post.getStatus())) {
            throw new IllegalArgumentException("当前单据不可创建会话");
        }

        ChatSession existing = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getPostId, postId)
                .eq(ChatSession::getDeleted, 0)
                .and(wrapper -> wrapper
                        .eq(ChatSession::getLostUserId, userId)
                        .or()
                        .eq(ChatSession::getFoundUserId, userId)), true);
        if (existing != null) {
            return toSessionResponse(existing, userId);
        }

        ChatSession session = new ChatSession();
        session.setPostId(postId);
        if (FOUND.equals(post.getPostType())) {
            session.setFoundUserId(post.getUserId());
            session.setLostUserId(userId);
        } else if (LOST.equals(post.getPostType())) {
            session.setLostUserId(post.getUserId());
            session.setFoundUserId(userId);
        } else {
            throw new IllegalArgumentException("单据类型不正确");
        }
        session.setStatus(ACTIVE);
        session.setDeleted(0);
        chatSessionMapper.insert(session);
        return toSessionResponse(session, userId);
    }

    /** 获取会话历史消息 */
    @Override
    public List<ChatMessageResponse> listMessages(Long userId, Long sessionId) {
        ChatSession session = requireParticipantSession(userId, sessionId);
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getDeleted, 0)
                        .orderByAsc(ChatMessage::getCreatedAt))
                .stream()
                .map(message -> toMessageResponse(message, session, userId))
                .toList();
    }

    /** 发送消息到会话，校验封禁和会话状态 */
    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long sessionId, String content) {
        AppUser user = appUserService.getByIdOrThrow(userId);
        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalArgumentException("封禁用户不能发送消息");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        ChatSession session = requireParticipantSession(userId, sessionId);
        if (!ACTIVE.equals(session.getStatus())) {
            throw new IllegalArgumentException("会话已关闭");
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderUserId(userId);
        message.setContent(content.trim());
        message.setMsgType("TEXT");
        message.setDeleted(0);
        chatMessageMapper.insert(message);
        return toMessageResponse(message, session, userId);
    }

    private ChatSession requireParticipantSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || Integer.valueOf(1).equals(session.getDeleted())) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!userId.equals(session.getLostUserId()) && !userId.equals(session.getFoundUserId())) {
            throw new IllegalArgumentException("只能访问自己的聊天会话");
        }
        return session;
    }

    private ChatSessionResponse toSessionResponse(ChatSession session, Long currentUserId) {
        ChatSessionResponse response = new ChatSessionResponse();
        response.setId(session.getId());
        response.setPostId(session.getPostId());
        response.setCurrentUserRole(roleOf(session, currentUserId));
        response.setPeerRole(LOST.equals(response.getCurrentUserRole()) ? FOUND : LOST);
        response.setStatus(session.getStatus());
        return response;
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message, ChatSession session, Long currentUserId) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSessionId(message.getSessionId());
        response.setSenderRole(roleOf(session, message.getSenderUserId()));
        response.setMine(currentUserId.equals(message.getSenderUserId()));
        response.setContent(message.getContent());
        response.setMsgType(message.getMsgType());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    private String roleOf(ChatSession session, Long userId) {
        if (userId.equals(session.getLostUserId())) return LOST;
        if (userId.equals(session.getFoundUserId())) return FOUND;
        throw new IllegalArgumentException("用户不属于该会话");
    }
}
