package com.shigui.service;

import com.shigui.dto.ChatMessageResponse;
import com.shigui.dto.ChatSessionResponse;
import com.shigui.dto.CreateChatSessionRequest;
import com.shigui.dto.SendMessageRequest;
import com.shigui.entity.AppUser;
import com.shigui.entity.ChatMessage;
import com.shigui.entity.ChatSession;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.ChatMessageMapper;
import com.shigui.mapper.ChatSessionMapper;
import com.shigui.mapper.ClaimRecordMapper;
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
    @Mock
    private ClaimRecordMapper claimRecordMapper;

    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(chatSessionMapper, chatMessageMapper, claimRecordMapper, lostFoundPostService, appUserService);
        injectBaseMapper(service, chatSessionMapper);
    }

    @Test
    void createOrGetSession_foundPostReturnsRoleViewWithoutUserIds() {
        LostFoundPost post = post("FOUND", 1L);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user("NORMAL"));
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(claimRecordMapper.selectCount(any())).thenReturn(1L); // 已有认领通过
        when(chatSessionMapper.selectOne(any(), eq(true))).thenReturn(null);
        when(chatSessionMapper.insert(any(ChatSession.class))).thenAnswer(inv -> {
            ChatSession session = inv.getArgument(0);
            session.setId(88L);
            return 1;
        });

        ChatSessionResponse response = service.createOrGetSession(2L, 10L);

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).insert(captor.capture());
        assertThat(captor.getValue().getFoundUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getLostUserId()).isEqualTo(2L);
        assertThat(response.getCurrentUserRole()).isEqualTo("LOST");
        assertThat(response.getPeerRole()).isEqualTo("FOUND");
    }

    @Test
    void getMessages_returnsMineAndSenderRoleInsteadOfUserId() {
        when(chatSessionMapper.selectById(88L)).thenReturn(session());
        when(chatMessageMapper.selectList(any())).thenReturn(List.of(message(7L, 2L, "你好"), message(8L, 1L, "请来取")));

        List<ChatMessageResponse> response = service.listMessages(2L, 88L);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getMine()).isTrue();
        assertThat(response.get(0).getSenderRole()).isEqualTo("LOST");
        assertThat(response.get(1).getMine()).isFalse();
        assertThat(response.get(1).getSenderRole()).isEqualTo("FOUND");
    }

    @Test
    void sendMessage_rejectsNonParticipant() {
        when(appUserService.getByIdOrThrow(3L)).thenReturn(user("NORMAL"));
        when(chatSessionMapper.selectById(88L)).thenReturn(session());

        assertThatThrownBy(() -> service.sendMessage(3L, 88L, "你好"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("自己的聊天会话");
    }

    private CreateChatSessionRequest request() {
        CreateChatSessionRequest request = new CreateChatSessionRequest();
        request.setPostId(10L);
        return request;
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

    private ChatSession session() {
        ChatSession session = new ChatSession();
        session.setId(88L);
        session.setPostId(10L);
        session.setLostUserId(2L);
        session.setFoundUserId(1L);
        session.setStatus("ACTIVE");
        session.setDeleted(0);
        return session;
    }

    private ChatMessage message(Long id, Long senderUserId, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setSessionId(88L);
        message.setSenderUserId(senderUserId);
        message.setContent(content);
        message.setMsgType("TEXT");
        return message;
    }

    private AppUser user(String status) {
        AppUser user = new AppUser();
        user.setId(2L);
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
