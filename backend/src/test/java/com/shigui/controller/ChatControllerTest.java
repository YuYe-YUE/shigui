package com.shigui.controller;

import com.shigui.dto.ChatMessageResponse;
import com.shigui.dto.ChatSessionResponse;
import com.shigui.service.AppUserService;
import com.shigui.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private AppUserService appUserService;

    @Test
    void createSession_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(post("/api/chat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createSession_loggedIn_returnsSessionWithoutUserIds() throws Exception {
        ChatSessionResponse response = new ChatSessionResponse();
        response.setId(99L);
        response.setPostId(10L);
        response.setCurrentUserRole("LOST");
        response.setPeerRole("FOUND");
        response.setStatus("ACTIVE");
        when(chatService.createOrGetSession(eq(2L), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/chat/sessions")
                        .header("satoken", getUserToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.currentUserRole").value("LOST"))
                .andExpect(jsonPath("$.data.peerRole").value("FOUND"))
                .andExpect(jsonPath("$.data.lostUserId").doesNotExist())
                .andExpect(jsonPath("$.data.foundUserId").doesNotExist());
    }

    @Test
    void messages_loggedIn_returnsList() throws Exception {
        ChatMessageResponse message = new ChatMessageResponse();
        message.setId(7L);
        message.setSessionId(99L);
        message.setSenderRole("LOST"); message.setMine(false);
        message.setContent("你好");
        message.setMsgType("TEXT");
        when(chatService.listMessages(2L, 99L)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/chat/sessions/99/messages")
                        .header("satoken", getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].content").value("你好"));
    }

    @Test
    void sendMessage_loggedIn_returnsMessage() throws Exception {
        ChatMessageResponse message = new ChatMessageResponse();
        message.setId(7L);
        message.setSessionId(99L);
        message.setSenderRole("LOST"); message.setMine(false);
        message.setContent("你好");
        message.setMsgType("TEXT");
        when(chatService.sendMessage(2L, 99L, "你好")).thenReturn(message);

        mockMvc.perform(post("/api/chat/sessions/99/messages")
                        .header("satoken", getUserToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.msgType").value("TEXT"));
    }

    private String getUserToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.setContext(request, response);
            cn.dev33.satoken.stp.StpUtil.login(2L);
            return cn.dev33.satoken.stp.StpUtil.getTokenValue();
        } finally {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.clearContext();
        }
    }
}
