package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.AdminMatchResponse;
import com.shigui.dto.MatchResponse;
import com.shigui.entity.MatchRecord;

/**
 * 匹配记录服务：AI 智能匹配生成、查询用户的匹配结果和管理端列表。
 */
public interface MatchRecordService extends IService<MatchRecord> {
    /** 单据审核通过后自动触发 AI 匹配，生成匹配记录并发送通知 */
    void generateMatchesForPost(Long postId);
    /** 获取当前用户的所有匹配记录（作为失主或拾捡者） */
    Page<MatchResponse> listMine(Long userId, int page, int size);
    /** 管理员查看全部匹配记录 */
    Page<AdminMatchResponse> listAdminMatches(int page, int size);
}
