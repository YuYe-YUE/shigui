package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.common.Result;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.service.ClaimRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
public class ClaimRecordController {

    private final ClaimRecordService claimRecordService;

    public ClaimRecordController(ClaimRecordService claimRecordService) {
        this.claimRecordService = claimRecordService;
    }

    @PostMapping
    public Result<ClaimResponse> create(@RequestBody CreateClaimRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(claimRecordService.createClaim(userId, request));
    }

    @GetMapping("/mine")
    public Result<Page<ClaimResponse>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(claimRecordService.listMine(userId, page, size));
    }

    @PutMapping("/{id}/confirm-receive")
    public Result<ClaimResponse> confirmReceive(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(claimRecordService.confirmReceive(userId, id));
    }
}
