package com.shigui.service;

import com.shigui.dto.ChatMessageResponse;
import com.shigui.dto.ChatSessionResponse;
import com.shigui.entity.AppUser;
import com.shigui.entity.ChatMessage;
import com.shigui.entity.ChatSession;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.ChatMessageMapper;
import com.shigui.mapper.ChatSessionMapper;
import com.shigui.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private LostFoundPostService lostFoundPostService;

    @Mock
    private AppUserService appUserService;

    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(chatMessageMapper, lostFoundPostService, appUserService);
        injectBaseMapper(service, chatSessionMapper);
    }

    @Test
    void createOrGetSession_foundPostOwnerIsFoundUserAndCurrentUserIsLostUser() {
        LostFoundPost post = post("FOUND", 1L);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user(2L, "NORMAL"));
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(chatSessionMapper.selectOne(any(), eq(true))).thenReturn(null);
        when(chatSessionMapper.insert(any(ChatSession.class))).thenAnswer(inv -> {
            ChatSession session = inv.getArgument(0);
            session.setId(99L);
            return 1;
        });

        ChatSessionResponse response = service.createOrGetSession(2L, 10L);

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).insert(captor.capture());
        assertThat(captor.getValue().getFoundUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getLostUserId()).isEqualTo(2L);
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getPostId()).isEqualTo(10L);
        assertThat(response.getCurrentUserRole()).isEqualTo("LOST");
        assertThat(response.getPeerRole()).isEqualTo("FOUND");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createOrGetSession_lostPostOwnerIsLostUserAndCurrentUserIsFoundUser() {
        LostFoundPost post = post("LOST", 1L);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user(2L, "NORMAL"));
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(chatSessionMapper.selectOne(any(), eq(true))).thenReturn(session(88L, 10L, 1L, 2L));

        ChatSessionResponse response = service.createOrGetSession(2L, 10L);

        assertThat(response.getId()).isEqualTo(88L);
        assertThat(response.getCurrentUserRole()).isEqualTo("FOUND");
        assertThat(response.getPeerRole()).isEqualTo("LOST");
    }

    @Test
    void createOrGetSession_rejectsPostOwnerAsThirdPartyForThisConversation() {
        LostFoundPost post = post("FOUND", 2L);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user(2L, "NORMAL"));
        when(lostFoundPostService.getById(10L)).thenReturn(post);

        assertThatThrownBy(() -> service.createOrGetSession(2L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能和自己创建聊天");
    }

    @Test
    void createOrGetSession_rejectsBannedUser() {
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user(2L, "BANNED"));

        assertThatThrownBy(() -> service.createOrGetSession(2L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("封禁用户不能创建聊天");
    }

    @Test
    void listMessages_participantGetsMessages() {
        when(chatSessionMapper.selectById(99L)).thenReturn(session(99L, 10L, 2L, 1L));
        when(chatMessageMapper.selectList(any())).thenReturn(List.of(message(7L, 99L, 2L, "你好")));

        List<ChatMessageResponse> messages = service.listMessages(2L, 99L);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getId()).isEqualTo(7L);
        assertThat(messages.get(0).getContent()).isEqualTo("你好");
        assertThat(messages.get(0).getMsgType()).isEqualTo("TEXT");
    }

    @Test
    void listMessages_rejectsNonParticipant() {
        when(chatSessionMapper.selectById(99L)).thenReturn(session(99L, 10L, 2L, 1L));

        assertThatThrownBy(() -> service.listMessages(3L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能访问自己的聊天会话");
    }

    @Test
    void sendMessage_participantSavesTextMessage() {
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user(2L, "NORMAL"));
        when(chatSessionMapper.selectById(99L)).thenReturn(session(99L, 10L, 2L, 1L));
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage message = inv.getArgument(0);
            message.setId(7L);
            return 1;
        });

        ChatMessageResponse response = service.sendMessage(2L, 99L, " 你好 ");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper).insert(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo(99L);
        assertThat(captor.getValue().getSenderUserId()).isEqualTo(2L);
        assertThat(captor.getValue().getContent()).isEqualTo("你好");
        assertThat(captor.getValue().getMsgType()).isEqualTo("TEXT");
        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getContent()).isEqualTo("你好");
        assertThat(response.getMsgType()).isEqualTo("TEXT");
    }

    @Test
    void sendMessage_rejectsBannedUser() {
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user(2L, "BANNED"));

        assertThatThrownBy(() -> service.sendMessage(2L, 99L, "你好"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("封禁用户不能发送消息");
    }

    @Test
    void sendMessage_rejectsNonParticipant() {
        when(appUserService.getByIdOrThrow(3L)).thenReturn(user(3L, "NORMAL"));
        when(chatSessionMapper.selectById(99L)).thenReturn(session(99L, 10L, 2L, 1L));

        assertThatThrownBy(() -> service.sendMessage(3L, 99L, "你好"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能访问自己的聊天会话");
    }

    private LostFoundPost post(String postType, Long userId) {
        LostFoundPost post = new LostFoundPost();
        post.setId(10L);
        post.setUserId(userId);
        post.setPostType(postType);
        post.setStatus("MATCHING");
        post.setDeleted(0);
        return post;
    }

    private ChatSession session(Long id, Long postId, Long lostUserId, Long foundUserId) {
        ChatSession session = new ChatSession();
        session.setId(id);
        session.setPostId(postId);
        session.setLostUserId(lostUserId);
        session.setFoundUserId(foundUserId);
        session.setStatus("ACTIVE");
        session.setDeleted(0);
        return session;
    }

    private ChatMessage message(Long id, Long sessionId, Long senderUserId, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setSessionId(sessionId);
        message.setSenderUserId(senderUserId);
        message.setContent(content);
        message.setMsgType("TEXT");
        message.setDeleted(0);
        return message;
    }

    private AppUser user(Long id, String status) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    private void injectBaseMapper(ChatServiceImpl target, ChatSessionMapper mapper) {
        try {
            Field field = findField(target.getClass(), "baseMapper");
            field.setAccessible(true);
            field.set(target, mapper);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
