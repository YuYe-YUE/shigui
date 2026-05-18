package com.shigui.dto;

import lombok.Data;

@Data
public class AdminDashboardResponse {
    private long registeredUsers;
    private long matchingPosts;
    private long todayPublished;
    private long successfulClaims;
    private long matchRecords;
}
