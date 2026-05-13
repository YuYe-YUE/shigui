package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.PostResponse;
import com.shigui.service.LostFoundPostService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class LostFoundPostController {

    private final LostFoundPostService lostFoundPostService;

    public LostFoundPostController(LostFoundPostService lostFoundPostService) {
        this.lostFoundPostService = lostFoundPostService;
    }

    @PostMapping
    public Result<PostResponse> publish(@RequestBody CreatePostRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(lostFoundPostService.publish(userId, request));
    }

    @GetMapping("/{id}")
    public Result<PostResponse> detail(@PathVariable Long id) {
        return Result.ok(lostFoundPostService.getDetail(id));
    }
}
