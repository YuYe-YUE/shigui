package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.entity.AuditRecord;

/**
 * 审核记录服务：记录管理员对单据的审核通过和删除操作日志。
 */
public interface AuditRecordService extends IService<AuditRecord> {
    /** 记录审核通过操作 */
    void logApprove(Long adminId, Long postId);
    /** 记录删除操作及原因 */
    void logDelete(Long adminId, Long postId, String reason);
}
