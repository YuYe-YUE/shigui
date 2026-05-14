package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.NotificationResponse;
import com.shigui.entity.Notification;

public interface NotificationService extends IService<Notification> {
    void createMatchNotification(Long userId, Long matchRecordId, String itemName, String campusArea, String score, String reason);
    Page<NotificationResponse> listMine(Long userId, int page, int size);
}
