package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.dto.NotificationResponse;
import com.shigui.entity.Notification;
import com.shigui.mapper.NotificationMapper;
import com.shigui.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知实现：创建匹配通知和查询用户通知列表。
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    /** 创建匹配通知：内容包含物品名称、校区、分数和脱敏后的原因 */
    @Override
    public void createMatchNotification(Long userId, Long matchRecordId, String itemName, String campusArea, String score, String reason) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType("MATCH");
        notification.setTitle("发现疑似匹配单据");
        notification.setContent("系统发现与你的" + itemName + "相关的疑似匹配，校区：" + campusArea + "，匹配分数：" + score + "。原因：" + sanitize(reason));
        notification.setRelatedId(matchRecordId);
        notification.setIsRead(0);
        notification.setDeleted(0);
        save(notification);
    }

    /** 获取用户通知列表（未读排前、按时间倒序） */
    @Override
    public Page<NotificationResponse> listMine(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.eq(Notification::getDeleted, 0);
        wrapper.orderByAsc(Notification::getIsRead).orderByDesc(Notification::getCreatedAt);
        Page<Notification> entityPage = page(new Page<>(page, size), wrapper);
        List<NotificationResponse> responses = entityPage.getRecords().stream().map(this::toResponse).toList();
        Page<NotificationResponse> result = new Page<>(page, size);
        result.setRecords(responses);
        result.setTotal(entityPage.getTotal());
        return result;
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setRelatedId(notification.getRelatedId());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("后四位\\d+", "私密特征匹配");
    }
}
