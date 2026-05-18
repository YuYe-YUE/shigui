package com.shigui.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.entity.AuditRecord;
import com.shigui.mapper.AuditRecordMapper;
import com.shigui.service.AuditRecordService;
import org.springframework.stereotype.Service;

/**
 * 审核记录实现：记录管理员审核通过和删除操作的审计日志。
 */
@Service
public class AuditRecordServiceImpl extends ServiceImpl<AuditRecordMapper, AuditRecord> implements AuditRecordService {

    /** 记录审核通过操作 */
    @Override
    public void logApprove(Long adminId, Long postId) {
        AuditRecord record = new AuditRecord();
        record.setAdminId(adminId);
        record.setPostId(postId);
        record.setAction("APPROVE");
        save(record);
    }

    /** 记录删除操作及原因 */
    @Override
    public void logDelete(Long adminId, Long postId, String reason) {
        AuditRecord record = new AuditRecord();
        record.setAdminId(adminId);
        record.setPostId(postId);
        record.setAction("DELETE");
        record.setReason(reason != null ? reason : "");
        save(record);
    }
}
