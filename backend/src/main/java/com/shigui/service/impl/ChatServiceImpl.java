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

@Service
public class ChatServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatService {

    private static final String ACTIVE = "ACTIVE";
    private static final String TEXT = "TEXT";

    private final ChatMessageMapper chatMessageMapper;
    private final LostFoundPostService lostFoundPostService;
    private final AppUserService appUserService;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper,
                           LostFoundPostService lostFoundPostService,
                           AppUserService appUserService) {
        this.chatMessageMapper = chatMessageMapper;
        this.lostFoundPostService = lostFoundPostService;
        this.appUserService = appUserService;
    }

    @Override
    @Transactional
    public ChatSessionResponse createOrGetSession(Long userId, Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("postId 不能为空");
        }
        ensureNotBanned(userId, "封禁用户不能创建聊天");

        LostFoundPost post = requirePost(postId);
        ChatSession existing = getOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getPostId, postId)
                .eq(ChatSession::getDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            ensureParticipant(existing, userId);
            return toSessionResponse(existing, userId);
        }

        if (post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("不能和自己创建聊天");
        }

        ChatSession session = new ChatSession();
        session.setPostId(postId);
        if ("FOUND".equals(post.getPostType())) {
            session.setFoundUserId(post.getUserId());
            session.setLostUserId(userId);
        } else if ("LOST".equals(post.getPostType())) {
            session.setLostUserId(post.getUserId());
            session.setFoundUserId(userId);
        } else {
            throw new IllegalArgumentException("单据类型不支持聊天");
        }
        session.setStatus(ACTIVE);
        session.setDeleted(0);
        save(session);
        return toSessionResponse(session, userId);
    }

    @Override
    public List<ChatMessageResponse> listMessages(Long userId, Long sessionId) {
        ChatSession session = requireSession(sessionId);
        ensureParticipant(session, userId);

        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getDeleted, 0)
                        .orderByAsc(ChatMessage::getCreatedAt)
                        .orderByAsc(ChatMessage::getId))
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long sessionId, String content) {
        ensureNotBanned(userId, "封禁用户不能发送消息");
        ChatSession session = requireSession(sessionId);
        ensureParticipant(session, userId);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderUserId(userId);
        message.setContent(content.trim());
        message.setMsgType(TEXT);
        message.setDeleted(0);
        chatMessageMapper.insert(message);
        return toMessageResponse(message);
    }

    private void ensureNotBanned(Long userId, String message) {
        AppUser user = appUserService.getByIdOrThrow(userId);
        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalArgumentException(message);
        }
    }

    private LostFoundPost requirePost(Long postId) {
        LostFoundPost post = lostFoundPostService.getById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())) {
            throw new IllegalArgumentException("单据不存在");
        }
        return post;
    }

    private ChatSession requireSession(Long sessionId) {
        ChatSession session = getById(sessionId);
        if (session == null || Integer.valueOf(1).equals(session.getDeleted())) {
            throw new IllegalArgumentException("聊天会话不存在");
        }
        return session;
    }

    private void ensureParticipant(ChatSession session, Long userId) {
        if (!userId.equals(session.getLostUserId()) && !userId.equals(session.getFoundUserId())) {
            throw new IllegalArgumentException("只能访问自己的聊天会话");
        }
    }

    private ChatSessionResponse toSessionResponse(ChatSession session, Long currentUserId) {
        ChatSessionResponse response = new ChatSessionResponse();
        response.setId(session.getId());
        response.setPostId(session.getPostId());
        response.setCurrentUserRole(currentUserId.equals(session.getLostUserId()) ? "LOST" : "FOUND");
        response.setPeerRole(currentUserId.equals(session.getLostUserId()) ? "FOUND" : "LOST");
        response.setStatus(session.getStatus());
        return response;
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSessionId(message.getSessionId());
        response.setSenderUserId(message.getSenderUserId());
        response.setContent(message.getContent());
        response.setMsgType(message.getMsgType());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
