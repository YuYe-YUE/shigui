package com.shigui.controller;

import com.shigui.common.Result;
import com.shigui.service.AdminUserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
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
}
