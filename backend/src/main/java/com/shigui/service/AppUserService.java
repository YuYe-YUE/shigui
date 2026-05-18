package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.entity.AppUser;

/**
 * 小程序用户服务：微信登录、查询、封禁/解封管理。
 */
public interface AppUserService extends IService<AppUser> {

    /**
     * 微信登录：已有 openid 则返回用户 id，否则创建默认用户后返回新 id。
     */
    Long loginByWechat(String openid);

    /**
     * 获取用户；不存在时抛业务异常，让统一异常处理返回 400。
     */
    AppUser getByIdOrThrow(Long userId);

    /** 封禁用户，不可再发布/认领/聊天 */
    void banUser(Long userId);

    /** 解封用户，恢复所有操作权限 */
    void unbanUser(Long userId);
}
