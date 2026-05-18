package com.shigui.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
/** 全局异常处理器，将各类异常统一转换为 Result<T> 格式返回 */
public class GlobalExceptionHandler {

    /**
     * Sa-Token 在未登录或 token 失效时会抛出这个异常，统一转换成前端好判断的 401 响应。
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(401, "未登录或登录已过期");
    }

    /**
     * 权限不足（非管理员调管理员接口）返回 403。
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotPermission(NotPermissionException e) {
        return Result.fail(403, "权限不足");
    }

    /**
     * 业务参数不合法时抛 IllegalArgumentException，统一返回 400 和具体提示。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBadRequest(IllegalArgumentException e) {
        return Result.fail(400, e.getMessage());
    }

    /**
     * 请求路径存在但 HTTP 方法不匹配时返回 405，例如用浏览器 GET 访问只支持 POST 的登录接口。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(405, "请求方法不支持");
    }

    /**
     * 非 API 静态资源不存在时返回 404，避免被兜底处理误判成服务器内部错误。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        return Result.fail(404, "资源不存在");
    }

    /**
     * 文件保存等系统状态异常，返回具体错误信息而非通用 500。
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleIllegalState(IllegalStateException e) {
        return Result.fail(500, e.getMessage());
    }

    /**
     * 兜底异常处理，避免把 Java 堆栈信息直接暴露给前端。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        return Result.fail(500, "服务器内部错误");
    }
}
