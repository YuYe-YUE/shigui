package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** chat_message 表，聊天消息内容 */
@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 自增主键
    private Long sessionId;          // 所属会话 ID
    private Long senderUserId;       // 发送方用户 ID
    private String content;          // 消息文本内容
    private String msgType;          // 消息类型：TEXT/IMAGE
    @TableLogic
    private Integer deleted;         // 逻辑删除标记
    private LocalDateTime createdAt; // 发送时间
}
