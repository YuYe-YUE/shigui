package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.entity.AdminUser;

public interface AdminUserService extends IService<AdminUser> {
    /**
     * 校验管理员账号密码，成功后返回 Sa-Token token。
     */
    String login(String username, String password);
}
