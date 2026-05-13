package com.shigui.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.entity.AppUser;
import com.shigui.mapper.AppUserMapper;
import com.shigui.service.AppUserService;
import org.springframework.stereotype.Service;

@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

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

    @Override
    public AppUser getByIdOrThrow(Long userId) {
        AppUser user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
        return user;
    }
}
