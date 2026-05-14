package com.shigui.service;

import com.shigui.entity.Notification;
import com.shigui.mapper.NotificationMapper;
import com.shigui.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    private NotificationMapper notificationMapper;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        notificationService = new NotificationServiceImpl();
        injectMapper(notificationService, notificationMapper);
    }

    @Test
    void createMatchNotification_savesUnreadMatchNotification() {
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(100L);
            assertThat(notification.getUserId()).isEqualTo(1L);
            assertThat(notification.getType()).isEqualTo("MATCH");
            assertThat(notification.getRelatedId()).isEqualTo(9L);
            assertThat(notification.getIsRead()).isZero();
            assertThat(notification.getDeleted()).isZero();
            assertThat(notification.getContent()).doesNotContain("后四位1234");
            return 1;
        });

        notificationService.createMatchNotification(1L, 9L, "校园卡", "南校园", "0.86", "校区和品类相似，时间接近。");

        verify(notificationMapper).insert(any(Notification.class));
    }

    private void injectMapper(NotificationServiceImpl service, NotificationMapper mapper) {
        try {
            // baseMapper is declared in CrudRepository, a parent of ServiceImpl
            Field field = findField(service.getClass(), "baseMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
