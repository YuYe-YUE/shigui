package com.shigui.common;

import lombok.Data;

/** 全局统一响应体，所有 Controller 接口均通过此类型返回 */
@Data
public class Result<T> {
    /**
     * 统一响应体。前端只需要按 code/message/data 三个字段处理所有接口结果。
     */
    private int code;
    private String message;
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
