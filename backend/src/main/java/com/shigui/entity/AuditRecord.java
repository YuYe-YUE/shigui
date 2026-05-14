package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audit_record")
public class AuditRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private Long postId;
    private String action;
    private String reason;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
