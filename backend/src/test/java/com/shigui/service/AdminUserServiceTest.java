package com.shigui.service;

import cn.dev33.satoken.stp.StpUtil;
import com.shigui.entity.AdminUser;
import com.shigui.mapper.AdminUserMapper;
import com.shigui.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    private AdminUserServiceImpl adminUserService;

    /**
     * 测试里复用和正式代码相同的 salt:sha256 格式，保证密码校验测的是实际规则。
     */
    private static String hashPassword(String raw) {
        SecureRandom rng = new SecureRandom();
        byte[] saltBytes = new byte[16];
        rng.nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((salt + raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return salt + ":" + sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl();
        try {
            // ServiceImpl 的 baseMapper 原本由 Spring/MyBatis 注入；单元测试里用反射放入 mock。
            java.lang.reflect.Field baseMapperField = null;
            Class<?> clazz = adminUserService.getClass();
            while (clazz != null && baseMapperField == null) {
                try {
                    baseMapperField = clazz.getDeclaredField("baseMapper");
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (baseMapperField == null) {
                throw new RuntimeException("baseMapper field not found in class hierarchy");
            }
            baseMapperField.setAccessible(true);
            baseMapperField.set(adminUserService, adminUserMapper);

            // lambdaQuery() 需要实体和 Mapper 类型；这里手动补齐，避免启动完整数据库环境。
            java.lang.reflect.Field entityClassField = com.baomidou.mybatisplus.extension.repository.AbstractRepository.class.getDeclaredField("entityClass");
            entityClassField.setAccessible(true);
            entityClassField.set(adminUserService, AdminUser.class);

            java.lang.reflect.Field mapperClassField = com.baomidou.mybatisplus.extension.repository.AbstractRepository.class.getDeclaredField("mapperClass");
            mapperClassField.setAccessible(true);
            mapperClassField.set(adminUserService, AdminUserMapper.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void login_correctCredentials_returnsToken() {
        // Sa-Token 是静态工具类；这里 mock 它的 token 返回值，只验证登录业务逻辑。
        String passwordHash = hashPassword("admin123");
        AdminUser admin = new AdminUser();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordHash);
        when(adminUserMapper.selectOne(any())).thenReturn(admin);

        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(() -> StpUtil.getTokenValue()).thenReturn("test-admin-token");
            String token = adminUserService.login("admin", "admin123");
            assertThat(token).isEqualTo("test-admin-token");
        }
    }

    @Test
    void login_wrongPassword_throwsException() {
        // 用户存在但密码不匹配时，应该给出统一的登录失败提示。
        String passwordHash = hashPassword("correct_password");
        AdminUser admin = new AdminUser();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordHash);
        when(adminUserMapper.selectOne(any())).thenReturn(admin);
        assertThatThrownBy(() -> adminUserService.login("admin", "wrong_password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void login_userNotFound_throwsException() {
        when(adminUserMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> adminUserService.login("nobody", "any"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名或密码错误");
    }
}
