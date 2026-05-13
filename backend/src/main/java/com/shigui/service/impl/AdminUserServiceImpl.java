package com.shigui.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.entity.AdminUser;
import com.shigui.mapper.AdminUserMapper;
import com.shigui.service.AdminUserService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {

    /**
     * 管理员登录成功后把管理员 id 写入 Sa-Token 会话，返回的 token 由前端保存并放到 satoken 请求头。
     */
    @Override
    public String login(String username, String password) {
        AdminUser admin = lambdaQuery().eq(AdminUser::getUsername, username).one();
        if (admin == null || !verifyPassword(password, admin.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        StpUtil.login(admin.getId());
        return StpUtil.getTokenValue();
    }

    /**
     * 校验 seed_data.sql 中的 salt:hash 密码格式。
     */
    private boolean verifyPassword(String rawPassword, String storedHash) {
        if (storedHash == null || !storedHash.contains(":")) return false;
        String[] parts = storedHash.split(":", 2);
        String salt = parts[0];
        String hash = parts[1];
        String computed = sha256(salt + rawPassword);
        return hash.equals(computed);
    }

    /**
     * 课程项目的最小密码哈希实现；真实生产系统应优先使用 BCrypt/Argon2。
     */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
