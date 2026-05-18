package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** chat_session 表，聊天会话（失主与拾捡者一对一） */
@Data
@TableName("chat_session")
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 自增主键
    private Long postId;             // 关联单据 ID
    private Long lostUserId;         // 失主用户 ID
    private Long foundUserId;        // 拾捡者用户 ID
    private String status;           // 会话状态：ACTIVE/CLOSED
    @TableLogic
    private Integer deleted;         // 逻辑删除标记
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
