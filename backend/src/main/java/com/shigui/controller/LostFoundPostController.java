package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.common.Result;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.MapPostResponse;
import com.shigui.dto.PostResponse;
import com.shigui.service.LostFoundPostService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 失物招领帖子接口，提供发布、查询筛选、我的记录和地图点位功能。
 */
@RestController
@RequestMapping("/api/posts")
public class LostFoundPostController {

    private final LostFoundPostService lostFoundPostService;

    public LostFoundPostController(LostFoundPostService lostFoundPostService) {
        this.lostFoundPostService = lostFoundPostService;
    }

    // 发布新帖子，须登录，新帖子进入待审核状态
    @PostMapping
    public Result<PostResponse> publish(@RequestBody CreatePostRequest request) {
        if (!StpUtil.isLogin()) {
            return Result.fail(401, "请先登录");
        }
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
        if (!StpUtil.isLogin()) {
            return Result.fail(401, "请先登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(lostFoundPostService.listMine(userId, page, size, postType));
    }

    /**
     * 地图点位（公开）。只返回有坐标的招领单，且不返回私密特征、暂存地点和用户信息。
     */
    @GetMapping("/map")
    public Result<List<MapPostResponse>> mapPoints() {
        return Result.ok(lostFoundPostService.listMapPosts());
    }

    // 获取帖子详情，已登录时包含发布者联系方式
    @GetMapping("/{id}")
    public Result<PostResponse> detail(@PathVariable Long id) {
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 0L;
        return Result.ok(lostFoundPostService.getDetail(id, currentUserId));
    }
}
