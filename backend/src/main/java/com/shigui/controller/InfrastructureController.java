package com.shigui.controller;

import com.shigui.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfrastructureController {

    /**
     * 方便直接打开 http://localhost:8080 确认后端进程还活着。
     */
    @GetMapping("/")
    public Result<String> health() {
        return Result.ok("拾归后端服务已启动");
    }

    /**
     * 浏览器会自动请求 favicon.ico；这里显式返回空响应，避免无意义的 404 日志。
     */
    @GetMapping("/favicon.ico")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favicon() {
    }
}
