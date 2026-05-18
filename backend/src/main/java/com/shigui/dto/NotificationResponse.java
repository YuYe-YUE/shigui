package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 通知消息响应 */
@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
