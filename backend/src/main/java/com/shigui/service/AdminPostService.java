package com.shigui.service;

/**
 * 管理员审核服务：处理单据审核通过和删除操作。
 */
public interface AdminPostService {
    /** 审核通过指定单据，触发匹配流程 */
    void approvePost(Long adminId, Long postId);
    /** 删除（下架）指定单据，记录删除原因 */
    void deletePost(Long adminId, Long postId, String reason);
}
