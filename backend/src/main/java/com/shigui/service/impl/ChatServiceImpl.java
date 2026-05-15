package com.shigui.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.dto.*;
import com.shigui.entity.*;
import com.shigui.mapper.ChatMessageMapper;
import com.shigui.mapper.ChatSessionMapper;
import com.shigui.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatService {
    private final ChatSessionMapper sessionMapper;
    private final LostFoundPostService lostFoundPostService;
    private final AppUserService appUserService;

    public ChatServiceImpl(ChatSessionMapper sessionMapper, LostFoundPostService lostFoundPostService,
                           AppUserService appUserService) {
        this.sessionMapper = sessionMapper;
        this.lostFoundPostService = lostFoundPostService;
        this.appUserService = appUserService;
    }

    @Override
    @Transactional
    public ChatSessionResponse createSession(Long userId, CreateChatSessionRequest request) {
        AppUser user = appUserService.getByIdOrThrow(userId);
        if ("BANNED".equals(user.getStatus())) throw new IllegalArgumentException("用户已被封禁");
        LostFoundPost post = lostFoundPostService.getById(request.getPostId());
        if (post == null) throw new IllegalArgumentException("单据不存在");
        if (!post.getUserId().equals(userId) && !"MATCHING".equals(post.getStatus()))
            throw new IllegalArgumentException("无权创建会话");

        ChatSession existing = sessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getPostId, request.getPostId())
                .eq(ChatSession::getDeleted, 0));
        if (existing != null) return toSessionResponse(existing);

        ChatSession session = new ChatSession();
        session.setPostId(request.getPostId());
        session.setLostUserId(post.getPostType().equals("LOST") ? post.getUserId() : userId);
        session.setFoundUserId(post.getPostType().equals("FOUND") ? post.getUserId() : userId);
        session.setStatus("ACTIVE");
        sessionMapper.insert(session);
        return toSessionResponse(session);
    }

    @Override
    public List<ChatMessageResponse> getMessages(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new IllegalArgumentException("会话不存在");
        if (!userId.equals(session.getLostUserId()) && !userId.equals(session.getFoundUserId()))
            throw new IllegalArgumentException("无权访问该会话");
        return list(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getDeleted, 0).orderByAsc(ChatMessage::getCreatedAt))
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long sessionId, Long userId, SendMessageRequest request) {
        AppUser user = appUserService.getByIdOrThrow(userId);
        if ("BANNED".equals(user.getStatus())) throw new IllegalArgumentException("用户已被封禁");
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new IllegalArgumentException("会话不存在");
        if (!userId.equals(session.getLostUserId()) && !userId.equals(session.getFoundUserId()))
            throw new IllegalArgumentException("无权发送消息");
        if (!"ACTIVE".equals(session.getStatus())) throw new IllegalArgumentException("会话已关闭");
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId); msg.setSenderUserId(userId);
        msg.setContent(request.getContent()); msg.setMsgType("TEXT");
        save(msg);
        return toResponse(msg);
    }

    private ChatSessionResponse toSessionResponse(ChatSession s) {
        ChatSessionResponse r = new ChatSessionResponse();
        r.setId(s.getId()); r.setPostId(s.getPostId());
        r.setLostUserId(s.getLostUserId()); r.setFoundUserId(s.getFoundUserId());
        r.setStatus(s.getStatus()); return r;
    }

    private ChatMessageResponse toResponse(ChatMessage m) {
        ChatMessageResponse r = new ChatMessageResponse();
        r.setId(m.getId()); r.setSessionId(m.getSessionId());
        r.setSenderUserId(m.getSenderUserId()); r.setContent(m.getContent());
        r.setMsgType(m.getMsgType()); r.setCreatedAt(m.getCreatedAt()); return r;
    }
}
