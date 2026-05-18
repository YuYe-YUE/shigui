package com.shigui.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.AdminClaimResponse;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.entity.ClaimRecord;

/**
 * 认领服务：处理认领申请、AI 预审、管理员审核、确认收件的完整状态机。
 */
public interface ClaimRecordService extends IService<ClaimRecord> {
    /** 失主提交认领申请，AI 预审后进入人工审核或自动通过/拒绝 */
    ClaimResponse createClaim(Long userId, CreateClaimRequest request);
    /** 获取当前用户的所有认领记录 */
    Page<ClaimResponse> listMine(Long userId, int page, int size);
    /** 根据 ID 获取认领详情 */
    ClaimResponse getByIdOrThrow(Long claimId);
    /** 确认收到失物，关闭认领并完成单据 */
    ClaimResponse confirmReceive(Long userId, Long claimId);
    /** 管理员分页查询所有认领申请 */
    Page<AdminClaimResponse> listAdminClaims(int page, int size, String status);
    /** 管理员审核通过认领申请 */
    AdminClaimResponse approveByAdmin(Long claimId);
    /** 管理员拒绝认领申请，单据状态回到匹配中 */
    AdminClaimResponse rejectByAdmin(Long claimId, String reason);
}
