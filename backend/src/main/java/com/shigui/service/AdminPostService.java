package com.shigui.service;

public interface AdminPostService {
    void approvePost(Long adminId, Long postId);
    void deletePost(Long adminId, Long postId, String reason);
}
