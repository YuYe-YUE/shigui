package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.entity.AuditRecord;

public interface AuditRecordService extends IService<AuditRecord> {
    void logApprove(Long adminId, Long postId);
    void logDelete(Long adminId, Long postId, String reason);
}
