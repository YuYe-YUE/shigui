package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.AdminMatchResponse;
import com.shigui.dto.MatchResponse;
import com.shigui.entity.MatchRecord;

public interface MatchRecordService extends IService<MatchRecord> {
    void generateMatchesForPost(Long postId);
    Page<MatchResponse> listMine(Long userId, int page, int size);
    Page<AdminMatchResponse> listAdminMatches(int page, int size);
}
