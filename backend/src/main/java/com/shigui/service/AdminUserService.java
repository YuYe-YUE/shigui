package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.entity.AdminUser;

public interface AdminUserService extends IService<AdminUser> {
    String login(String username, String password);
}
