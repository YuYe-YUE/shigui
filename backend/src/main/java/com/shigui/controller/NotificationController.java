package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.common.Result;
import com.shigui.dto.NotificationResponse;
import com.shigui.service.NotificationService;
import org.springframework.web.bind.annotation.*;

/**
 * 通知接口，提供当前用户的通知列表查询。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 分页查询当前用户的通知列表
    @GetMapping
    public Result<Page<NotificationResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(notificationService.listMine(userId, page, size));
    }
}
