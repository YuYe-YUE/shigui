package com.shigui.controller;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.common.Result;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.service.ClaimRecordService;
import org.springframework.web.bind.annotation.*;

/**
 * 认领记录接口，提供发起认领、查询我的认领、确认收到等功能。
 */
@RestController
@RequestMapping("/api/claims")
public class ClaimRecordController {
    private final ClaimRecordService claimRecordService;
    public ClaimRecordController(ClaimRecordService claimRecordService) { this.claimRecordService = claimRecordService; }

    // 发起认领申请，需登录
    @PostMapping
    public Result<ClaimResponse> create(@RequestBody CreateClaimRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(claimRecordService.createClaim(userId, request));
    }

    // 分页查询当前用户发起的认领记录
    @GetMapping("/mine")
    public Result<Page<ClaimResponse>> mine(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(claimRecordService.listMine(StpUtil.getLoginIdAsLong(), page, size));
    }

    // 获取认领记录详情，仅限申请人本人查看
    @GetMapping("/{id}")
    public Result<ClaimResponse> detail(@PathVariable Long id) {
        return Result.ok(claimRecordService.getByIdOrThrow(id, StpUtil.getLoginIdAsLong()));
    }

    // 确认收到物品，更新认领状态为已完成
    @PutMapping("/{id}/confirm-receive")
    public Result<ClaimResponse> confirmReceive(@PathVariable Long id) {
        return Result.ok(claimRecordService.confirmReceive(StpUtil.getLoginIdAsLong(), id));
    }
}
