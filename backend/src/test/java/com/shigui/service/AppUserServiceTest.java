package com.shigui.service;

import com.shigui.entity.AppUser;
import com.shigui.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserMapper appUserMapper;

    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = new com.shigui.service.impl.AppUserServiceImpl();
        try {
            // ServiceImpl 的 baseMapper 原本由 Spring/MyBatis 注入；单元测试里用反射放入 mock。
            java.lang.reflect.Field baseMapperField = null;
            Class<?> clazz = appUserService.getClass();
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
            baseMapperField.set(appUserService, appUserMapper);

            // lambdaQuery() 需要实体和 Mapper 类型；这里手动补齐，避免启动完整数据库环境。
            java.lang.reflect.Field entityClassField = com.baomidou.mybatisplus.extension.repository.AbstractRepository.class.getDeclaredField("entityClass");
            entityClassField.setAccessible(true);
            entityClassField.set(appUserService, AppUser.class);

            java.lang.reflect.Field mapperClassField = com.baomidou.mybatisplus.extension.repository.AbstractRepository.class.getDeclaredField("mapperClass");
            mapperClassField.setAccessible(true);
            mapperClassField.set(appUserService, AppUserMapper.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void loginByWechat_existingUser_returnsId() {
        AppUser existing = new AppUser();
        existing.setId(1L);
        existing.setOpenid("wx_openid_123");

        when(appUserMapper.selectOne(any())).thenReturn(existing);

        Long id = appUserService.loginByWechat("wx_openid_123");
        assertThat(id).isEqualTo(1L);
    }

    @Test
    void loginByWechat_newUser_createsAndReturnsId() {
        // 新用户必须带 NORMAL 状态，这是后续封禁逻辑的默认基线。
        when(appUserMapper.selectOne(any())).thenReturn(null);
        when(appUserMapper.insert(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            assertThat(u.getStatus()).isEqualTo("NORMAL");
            u.setId(2L);
            return 1;
        });

        Long id = appUserService.loginByWechat("wx_new_openid");
        assertThat(id).isEqualTo(2L);
    }

    @Test
    void getByIdOrThrow_existingUser_returnsUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setNickname("test");

        when(appUserMapper.selectById(1L)).thenReturn(user);

        AppUser result = appUserService.getByIdOrThrow(1L);
        assertThat(result.getNickname()).isEqualTo("test");
    }

    @Test
    void getByIdOrThrow_notFound_throwsException() {
        when(appUserMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> appUserService.getByIdOrThrow(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户不存在");
    }
}
