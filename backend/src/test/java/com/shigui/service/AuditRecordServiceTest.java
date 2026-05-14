package com.shigui.service;

import com.shigui.entity.AuditRecord;
import com.shigui.mapper.AuditRecordMapper;
import com.shigui.service.impl.AuditRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditRecordServiceTest {

    private AuditRecordMapper auditRecordMapper;
    private AuditRecordService auditRecordService;

    @BeforeEach
    void setUp() {
        auditRecordMapper = mock(AuditRecordMapper.class);
        auditRecordService = new AuditRecordServiceImpl();
        injectMapper(auditRecordService, auditRecordMapper);
    }

    @Test
    void logApprove_createsApproveRecord() {
        auditRecordService.logApprove(1L, 100L);
        verify(auditRecordMapper).insert(any(AuditRecord.class));
    }

    @Test
    void logDelete_createsDeleteRecordWithReason() {
        auditRecordService.logDelete(1L, 100L, "违规内容");
        verify(auditRecordMapper).insert(any(AuditRecord.class));
    }

    private void injectMapper(Object service, Object mapper) {
        try {
            var field = com.baomidou.mybatisplus.extension.repository.CrudRepository.class.getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
