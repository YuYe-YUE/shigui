package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** audit_record 表，管理员审核操作记录 */
@Data
@TableName("audit_record")
public class AuditRecord {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 自增主键
    private Long adminId;            // 审核管理员 ID
    private Long postId;             // 被审核单据 ID
    private String action;           // 审核动作：APPROVED/REJECTED
    private String reason;           // 审核原因/备注
    @TableLogic
    private Integer deleted;         // 逻辑删除标记
    private LocalDateTime createdAt; // 审核时间
}
