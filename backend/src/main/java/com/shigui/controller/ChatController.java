package com.shigui.controller;
import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.dto.*;
import com.shigui.service.ChatService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    public ChatController(ChatService chatService) { this.chatService = chatService; }

    @PostMapping("/sessions")
    public Result<ChatSessionResponse> createSession(@RequestBody CreateChatSessionRequest request) {
        return Result.ok(chatService.createOrGetSession(StpUtil.getLoginIdAsLong(), request.getPostId()));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessageResponse>> getMessages(@PathVariable Long id) {
        return Result.ok(chatService.listMessages(StpUtil.getLoginIdAsLong(), id));
    }

    @PostMapping("/sessions/{id}/messages")
    public Result<ChatMessageResponse> sendMessage(@PathVariable Long id, @RequestBody SendMessageRequest request) {
        return Result.ok(chatService.sendMessage(StpUtil.getLoginIdAsLong(), id, request.getContent()));
    }
}
