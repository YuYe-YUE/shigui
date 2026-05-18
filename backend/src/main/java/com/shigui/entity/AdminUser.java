package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** admin_user 表，管理员账户 */
@Data
@TableName("admin_user")
public class AdminUser {
    @TableId(type = IdType.AUTO)
    private Long id;                       // 自增主键
    private String username;               // 登录用户名
    /**
     * 格式为 salt:sha256(salt + password)，避免明文保存管理员密码。
     */
    private String passwordHash;
    @TableLogic
    private Integer deleted;               // 逻辑删除标记
    private LocalDateTime createdAt;       // 创建时间
    private LocalDateTime updatedAt;       // 更新时间
}
