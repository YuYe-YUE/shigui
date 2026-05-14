package com.shigui.controller;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.entity.AppUser;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.AdminUserService;
import com.shigui.service.AppUserService;
import com.shigui.service.AuditRecordService;
import com.shigui.service.LostFoundPostService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final long ADMIN_ID_OFFSET = 10_000_000L;

    private final AdminUserService adminUserService;
    private final LostFoundPostService lostFoundPostService;
    private final AuditRecordService auditRecordService;
    private final AppUserService appUserService;

    public AdminController(AdminUserService adminUserService,
                           LostFoundPostService lostFoundPostService,
                           AuditRecordService auditRecordService,
                           AppUserService appUserService) {
        this.adminUserService = adminUserService;
        this.lostFoundPostService = lostFoundPostService;
        this.auditRecordService = auditRecordService;
        this.appUserService = appUserService;
    }

    /**
     * 校验当前登录是否为管理员。普通用户 token 会抛出 NotPermissionException → 403。
     */
    private void requireAdmin() {
        if (StpUtil.getLoginIdAsLong() < ADMIN_ID_OFFSET) {
            throw new NotPermissionException("需要管理员权限");
        }
    }

    /**
     * 管理端登录入口。Controller 只做参数检查和响应包装，密码校验交给 Service。
     */
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
        Long adminId = StpUtil.getLoginIdAsLong() - ADMIN_ID_OFFSET;
        LostFoundPost post = lostFoundPostService.getById(id);
        if (post == null) return Result.fail(404, "单据不存在");
        if (post.getDeleted() != null && post.getDeleted() == 1) return Result.fail(400, "单据已被删除");
        if (!"PENDING_AUDIT".equals(post.getStatus())) return Result.fail(400, "只能审核待审核状态的单据");
        post.setStatus("MATCHING");
        lostFoundPostService.updateById(post);
        auditRecordService.logApprove(adminId, id);
        return Result.ok();
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireAdmin();
        Long adminId = StpUtil.getLoginIdAsLong() - ADMIN_ID_OFFSET;
        LostFoundPost post = lostFoundPostService.getById(id);
        if (post == null) return Result.fail(404, "单据不存在");
        if (post.getDeleted() != null && post.getDeleted() == 1) return Result.fail(400, "单据已被删除");
        String reason = body.getOrDefault("reason", "");
        if (reason.isBlank()) return Result.fail(400, "删除原因不能为空");
        post.setDeleted(1);
        lostFoundPostService.updateById(post);
        auditRecordService.logDelete(adminId, id, reason);
        return Result.ok();
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
}
