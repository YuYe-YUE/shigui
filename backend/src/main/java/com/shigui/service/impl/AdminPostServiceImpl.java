package com.shigui.service.impl;

import com.shigui.entity.LostFoundPost;
import com.shigui.service.AdminPostService;
import com.shigui.service.AuditRecordService;
import com.shigui.service.LostFoundPostService;
import com.shigui.service.MatchRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPostServiceImpl implements AdminPostService {

    private final LostFoundPostService lostFoundPostService;
    private final AuditRecordService auditRecordService;
    private final MatchRecordService matchRecordService;

    public AdminPostServiceImpl(LostFoundPostService lostFoundPostService,
                                AuditRecordService auditRecordService,
                                MatchRecordService matchRecordService) {
        this.lostFoundPostService = lostFoundPostService;
        this.auditRecordService = auditRecordService;
        this.matchRecordService = matchRecordService;
    }

    @Override
    @Transactional
    public void approvePost(Long adminId, Long postId) {
        LostFoundPost post = lostFoundPostService.getById(postId);
        if (post == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        if (post.getDeleted() != null && post.getDeleted() == 1) {
            throw new IllegalArgumentException("单据已被删除");
        }
        if (!"PENDING_AUDIT".equals(post.getStatus())) {
            throw new IllegalArgumentException("只能审核待审核状态的单据");
        }
        post.setStatus("MATCHING");
        lostFoundPostService.updateById(post);
        auditRecordService.logApprove(adminId, postId);
        matchRecordService.generateMatchesForPost(postId);
    }

    @Override
    @Transactional
    public void deletePost(Long adminId, Long postId, String reason) {
        LostFoundPost post = lostFoundPostService.getById(postId);
        if (post == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        if (post.getDeleted() != null && post.getDeleted() == 1) {
            throw new IllegalArgumentException("单据已被删除");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("删除原因不能为空");
        }
        post.setDeleted(1);
        lostFoundPostService.updateById(post);
        auditRecordService.logDelete(adminId, postId, reason);
    }
}
