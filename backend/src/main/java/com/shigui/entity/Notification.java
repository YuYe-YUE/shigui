package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** notification 表，用户通知消息 */
@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 自增主键
    private Long userId;             // 接收用户 ID
    private String type;             // 通知类型：MATCH/CLAIM/MESSAGE
    private String title;            // 通知标题
    private String content;          // 通知内容
    private Long relatedId;          // 关联业务 ID（如匹配 ID）
    private Integer isRead;          // 是否已读：0/1
    @TableLogic
    private Integer deleted;         // 逻辑删除标记
    private LocalDateTime createdAt; // 创建时间
}
