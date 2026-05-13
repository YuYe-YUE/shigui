package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.entity.AppUser;

public interface AppUserService extends IService<AppUser> {

    Long loginByWechat(String openid);

    AppUser getByIdOrThrow(Long userId);
}
