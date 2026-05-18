package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** app_user 表，微信小程序用户 */
@Data
@TableName("app_user")
public class AppUser {
    @TableId(type = IdType.AUTO)
    private Long id;                       // 自增主键
    private String openid;                 // 微信 OpenID
    private String nickname;               // 微信昵称
    private String avatarUrl;              // 微信头像 URL
    private String role;                   // 用户角色：LOST/FOUND
    /**
     * NORMAL 表示可正常操作，BANNED 表示只能登录查看历史记录，不能发布/认领/聊天。
     */
    private String status;
    @TableLogic
    private Integer deleted;               // 逻辑删除标记
    private LocalDateTime createdAt;       // 创建时间
    private LocalDateTime updatedAt;       // 更新时间
}
