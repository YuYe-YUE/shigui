package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.dto.ChatMessageResponse;
import com.shigui.dto.ChatSessionResponse;
import com.shigui.dto.CreateChatSessionRequest;
import com.shigui.dto.SendMessageRequest;
import com.shigui.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    public Result<ChatSessionResponse> createSession(@RequestBody CreateChatSessionRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(chatService.createOrGetSession(userId, request.getPostId()));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessageResponse>> messages(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(chatService.listMessages(userId, id));
    }

    @PostMapping("/sessions/{id}/messages")
    public Result<ChatMessageResponse> send(@PathVariable Long id, @RequestBody SendMessageRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(chatService.sendMessage(userId, id, request.getContent()));
    }
}
