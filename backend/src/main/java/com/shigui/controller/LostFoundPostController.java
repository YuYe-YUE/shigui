package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 列表筛选、搜索、分页。只返回 status=MATCHING 且未删除的单据。
     */
    @GetMapping
    public Result<Page<PostResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String postType,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String campusArea,
            @RequestParam(required = false) String keyword) {
        return Result.ok(lostFoundPostService.listPublic(page, size, postType, itemCategory, campusArea, keyword));
    }

    /**
     * 我的记录：查看当前用户发布的所有单据。返回所有状态、未删除的单据。
     */
    @GetMapping("/mine")
    public Result<Page<PostResponse>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String postType) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(lostFoundPostService.listMine(userId, page, size, postType));
    }

    /**
     * 地图点位（公开）。S7 完整实现，S3 返回空数组占位避免 500。
     */
    @GetMapping("/map")
    public Result<java.util.List<Object>> mapPoints() {
        return Result.ok(java.util.List.of());
    }

    @GetMapping("/{id}")
    public Result<PostResponse> detail(@PathVariable Long id) {
        return Result.ok(lostFoundPostService.getDetail(id));
    }
}
