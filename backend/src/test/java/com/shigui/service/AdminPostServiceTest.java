package com.shigui.service;

import com.shigui.entity.LostFoundPost;
import com.shigui.service.impl.AdminPostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPostServiceTest {

    @Mock
    private LostFoundPostService lostFoundPostService;

    @Mock
    private AuditRecordService auditRecordService;

    @Mock
    private MatchRecordService matchRecordService;

    private AdminPostService adminPostService;

    @BeforeEach
    void setUp() {
        adminPostService = new AdminPostServiceImpl(lostFoundPostService, auditRecordService, matchRecordService);
    }

    @Test
    void approvePost_success() {
        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        post.setStatus("PENDING_AUDIT");
        when(lostFoundPostService.getById(1L)).thenReturn(post);

        adminPostService.approvePost(1L, 1L);

        verify(lostFoundPostService).updateById(post);
        verify(auditRecordService).logApprove(1L, 1L);
        verify(matchRecordService).generateMatchesForPost(1L);
    }

    @Test
    void approvePost_notFound_throws() {
        when(lostFoundPostService.getById(99L)).thenReturn(null);

        assertThatThrownBy(() -> adminPostService.approvePost(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("单据不存在");
    }

    @Test
    void approvePost_deleted_throws() {
        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        post.setDeleted(1);
        when(lostFoundPostService.getById(1L)).thenReturn(post);

        assertThatThrownBy(() -> adminPostService.approvePost(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已被删除");
    }

    @Test
    void approvePost_notPendingAudit_throws() {
        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        post.setStatus("MATCHING");
        when(lostFoundPostService.getById(1L)).thenReturn(post);

        assertThatThrownBy(() -> adminPostService.approvePost(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能审核待审核");
    }

    @Test
    void approvePost_auditRecordLogsOnStatusChange() {
        // 验证：状态变更和日志记录是一个事务（两个操作都被调用）
        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        post.setStatus("PENDING_AUDIT");
        when(lostFoundPostService.getById(1L)).thenReturn(post);

        adminPostService.approvePost(1L, 1L);

        verify(lostFoundPostService).updateById(post);
        verify(auditRecordService).logApprove(eq(1L), eq(1L));
    }

    @Test
    void deletePost_success() {
        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        when(lostFoundPostService.getById(1L)).thenReturn(post);

        adminPostService.deletePost(1L, 1L, "违规内容");

        verify(lostFoundPostService).updateById(post);
        verify(auditRecordService).logDelete(1L, 1L, "违规内容");
    }

    @Test
    void deletePost_noReason_throws() {
        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        when(lostFoundPostService.getById(1L)).thenReturn(post);

        assertThatThrownBy(() -> adminPostService.deletePost(1L, 1L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("删除原因不能为空");
    }
}
