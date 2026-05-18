package com.shigui.controller;
import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.dto.*;
import com.shigui.service.ChatService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 即时聊天接口，提供创建会话、发送消息和查询消息列表功能。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    public ChatController(ChatService chatService) { this.chatService = chatService; }

    // 创建或获取与某帖子发布者的聊天会话
    @PostMapping("/sessions")
    public Result<ChatSessionResponse> createSession(@RequestBody CreateChatSessionRequest request) {
        return Result.ok(chatService.createOrGetSession(StpUtil.getLoginIdAsLong(), request.getPostId()));
    }

    // 获取指定会话的消息列表
    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessageResponse>> getMessages(@PathVariable Long id) {
        return Result.ok(chatService.listMessages(StpUtil.getLoginIdAsLong(), id));
    }

    // 向指定会话发送一条消息
    @PostMapping("/sessions/{id}/messages")
    public Result<ChatMessageResponse> sendMessage(@PathVariable Long id, @RequestBody SendMessageRequest request) {
        return Result.ok(chatService.sendMessage(StpUtil.getLoginIdAsLong(), id, request.getContent()));
    }
}
