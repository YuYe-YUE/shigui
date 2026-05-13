package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.entity.AppUser;
import com.shigui.service.AppUserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping("/wx-login")
    public Result<String> wxLogin(@RequestBody Map<String, String> body) {
        String openid = body.getOrDefault("openid", "");
        if (openid.isBlank()) {
            return Result.fail(400, "openid 不能为空");
        }
        Long userId = appUserService.loginByWechat(openid);
        StpUtil.login(userId);
        return Result.ok(StpUtil.getTokenValue());
    }

    @GetMapping("/me")
    public Result<AppUser> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(appUserService.getByIdOrThrow(userId));
    }
}
