package com.shigui.controller;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.common.Result;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.service.ClaimRecordService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
public class ClaimRecordController {
    private final ClaimRecordService claimRecordService;
    public ClaimRecordController(ClaimRecordService claimRecordService) { this.claimRecordService = claimRecordService; }

    @PostMapping
    public Result<ClaimResponse> create(@RequestBody CreateClaimRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(claimRecordService.createClaim(userId, request));
    }

    @GetMapping("/mine")
    public Result<Page<ClaimResponse>> mine(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(claimRecordService.listMine(StpUtil.getLoginIdAsLong(), page, size));
    }

    @GetMapping("/{id}")
    public Result<ClaimResponse> detail(@PathVariable Long id) {
        return Result.ok(claimRecordService.getByIdOrThrow(id));
    }

    @PutMapping("/{id}/confirm-receive")
    public Result<ClaimResponse> confirmReceive(@PathVariable Long id) {
        return Result.ok(claimRecordService.confirmReceive(StpUtil.getLoginIdAsLong(), id));
    }
}
