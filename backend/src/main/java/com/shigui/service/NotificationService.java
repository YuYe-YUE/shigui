package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.NotificationResponse;
import com.shigui.entity.Notification;

/**
 * 通知服务：匹配成功的通知创建和用户通知列表查询。
 */
public interface NotificationService extends IService<Notification> {
    /** 创建一条匹配成功通知，推送给匹配双方用户 */
    void createMatchNotification(Long userId, Long matchRecordId, String itemName, String campusArea, String score, String reason);
    /** 获取当前用户的通知列表（未读在前） */
    Page<NotificationResponse> listMine(Long userId, int page, int size);
}
