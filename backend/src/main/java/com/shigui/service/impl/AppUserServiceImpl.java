package com.shigui.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.entity.AppUser;
import com.shigui.mapper.AppUserMapper;
import com.shigui.service.AppUserService;
import org.springframework.stereotype.Service;

@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

    /**
     * 用 openid 作为小程序用户的唯一身份。首次登录时创建 NORMAL 状态的普通用户。
     */
    @Override
    public Long loginByWechat(String openid) {
        AppUser existing = lambdaQuery().eq(AppUser::getOpenid, openid).one();
        if (existing != null) {
            return existing.getId();
        }
        AppUser newUser = new AppUser();
        newUser.setOpenid(openid);
        newUser.setNickname("微信用户");
        newUser.setRole("USER");
        newUser.setStatus("NORMAL");
        save(newUser);
        return newUser.getId();
    }

    /**
     * 给 Controller 提供“必须存在”的读取方法，避免每个接口重复写空值判断。
     */
    @Override
    public AppUser getByIdOrThrow(Long userId) {
        AppUser user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
        return user;
    }
}
