package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class AppUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String nickname;
    private String avatarUrl;
    private String role;
    /**
     * NORMAL 表示可正常操作，BANNED 表示只能登录查看历史记录，不能发布/认领/聊天。
     */
    private String status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
