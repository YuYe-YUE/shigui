package com.shigui.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.entity.ClaimRecord;

public interface ClaimRecordService extends IService<ClaimRecord> {
    ClaimResponse createClaim(Long userId, CreateClaimRequest request);
    Page<ClaimResponse> listMine(Long userId, int page, int size);
    ClaimResponse getByIdOrThrow(Long claimId);
    void confirmReceive(Long claimId, Long userId);
}
