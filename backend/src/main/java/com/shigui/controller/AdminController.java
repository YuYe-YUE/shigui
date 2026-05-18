package com.shigui.controller;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.dto.AdminDashboardResponse;
import com.shigui.dto.AdminMatchResponse;
import com.shigui.entity.AppUser;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.AdminPostService;
import com.shigui.service.AdminUserService;
import com.shigui.service.AppUserService;
import com.shigui.service.LostFoundPostService;
import com.shigui.dto.AdminClaimResponse;
import com.shigui.dto.RejectClaimRequest;
import com.shigui.service.ClaimRecordService;
import com.shigui.service.MatchRecordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final long ADMIN_ID_OFFSET = 10_000_000L;

    private final AdminUserService adminUserService;
    private final LostFoundPostService lostFoundPostService;
    private final AdminPostService adminPostService;
    private final AppUserService appUserService;
    private final MatchRecordService matchRecordService;
    private final ClaimRecordService claimRecordService;

    public AdminController(AdminUserService adminUserService,
                           LostFoundPostService lostFoundPostService,
                           AdminPostService adminPostService,
                           AppUserService appUserService,
                           MatchRecordService matchRecordService,
                           ClaimRecordService claimRecordService) {
        this.adminUserService = adminUserService;
        this.lostFoundPostService = lostFoundPostService;
        this.adminPostService = adminPostService;
        this.appUserService = appUserService;
        this.matchRecordService = matchRecordService;
        this.claimRecordService = claimRecordService;
    }

    private void requireAdmin() {
        if (StpUtil.getLoginIdAsLong() < ADMIN_ID_OFFSET) {
            throw new NotPermissionException("需要管理员权限");
        }
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        if (username.isBlank() || password.isBlank()) {
            return Result.fail(400, "用户名和密码不能为空");
        }
        String token = adminUserService.login(username, password);
        return Result.ok(token);
    }

    @GetMapping("/posts")
    public Result<Page<LostFoundPost>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        requireAdmin();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LostFoundPost> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(status != null && !status.isEmpty(), LostFoundPost::getStatus, status);
        wrapper.orderByDesc(LostFoundPost::getPublishedAt);
        Page<LostFoundPost> result = lostFoundPostService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(p -> p.setPrivateFeature(null));
        return Result.ok(result);
    }

    @GetMapping("/posts/{id}")
    public Result<LostFoundPost> postDetail(@PathVariable Long id) {
        requireAdmin();
        LostFoundPost post = lostFoundPostService.getById(id);
        if (post == null) return Result.fail(404, "单据不存在");
        return Result.ok(post);
    }

    @PostMapping("/posts/{id}/approve")
    public Result<Void> approvePost(@PathVariable Long id) {
        requireAdmin();
        try {
            Long adminId = StpUtil.getLoginIdAsLong() - ADMIN_ID_OFFSET;
            adminPostService.approvePost(adminId, id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireAdmin();
        try {
            Long adminId = StpUtil.getLoginIdAsLong() - ADMIN_ID_OFFSET;
            String reason = body.getOrDefault("reason", "");
            adminPostService.deletePost(adminId, id, reason);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @GetMapping("/users")
    public Result<Page<AppUser>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        requireAdmin();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppUser> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(status != null && !status.isEmpty(), AppUser::getStatus, status);
        return Result.ok(appUserService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/matches")
    public Result<Page<AdminMatchResponse>> listMatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireAdmin();
        return Result.ok(matchRecordService.listAdminMatches(page, size));
    }

    @GetMapping("/dashboard")
    public Result<AdminDashboardResponse> dashboard() {
        requireAdmin();
        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setRegisteredUsers(appUserService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getDeleted, 0)));
        response.setMatchingPosts(lostFoundPostService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LostFoundPost>()
                .eq(LostFoundPost::getDeleted, 0)
                .eq(LostFoundPost::getStatus, "MATCHING")));
        response.setTodayPublished(lostFoundPostService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LostFoundPost>()
                .eq(LostFoundPost::getDeleted, 0)
                .ge(LostFoundPost::getPublishedAt, LocalDate.now().atStartOfDay())));
        response.setSuccessfulClaims(lostFoundPostService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LostFoundPost>()
                .eq(LostFoundPost::getDeleted, 0)
                .eq(LostFoundPost::getStatus, "COMPLETED")));
        response.setMatchRecords(matchRecordService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.shigui.entity.MatchRecord>()
                .eq(com.shigui.entity.MatchRecord::getDeleted, 0)));
        return Result.ok(response);
    }

    @PutMapping("/users/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id) {
        requireAdmin();
        appUserService.banUser(id);
        return Result.ok();
    }

    @PutMapping("/users/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        requireAdmin();
        appUserService.unbanUser(id);
        return Result.ok();
    }

    @GetMapping("/claims")
    public Result<Page<AdminClaimResponse>> listClaims(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        requireAdmin();
        return Result.ok(claimRecordService.listAdminClaims(page, size, status));
    }

    @PutMapping("/claims/{id}/approve")
    public Result<AdminClaimResponse> approveClaim(@PathVariable Long id) {
        requireAdmin();
        return Result.ok(claimRecordService.approveByAdmin(id));
    }

    @PutMapping("/claims/{id}/reject")
    public Result<AdminClaimResponse> rejectClaim(@PathVariable Long id, @RequestBody RejectClaimRequest request) {
        requireAdmin();
        return Result.ok(claimRecordService.rejectByAdmin(id, request == null ? "" : request.getReason()));
    }
}
