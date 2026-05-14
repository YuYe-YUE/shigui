package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.common.Result;
import com.shigui.dto.MatchResponse;
import com.shigui.service.MatchRecordService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class MatchRecordController {

    private final MatchRecordService matchRecordService;

    public MatchRecordController(MatchRecordService matchRecordService) {
        this.matchRecordService = matchRecordService;
    }

    @GetMapping("/mine")
    public Result<Page<MatchResponse>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(matchRecordService.listMine(userId, page, size));
    }
}
