package com.shigui.dto;

import lombok.Data;

/** 管理端仪表盘统计数据响应 */
@Data
public class AdminDashboardResponse {
    private long registeredUsers;
    private long matchingPosts;
    private long todayPublished;
    private long successfulClaims;
    private long matchRecords;
}
