package com.shigui.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.*;
import com.shigui.entity.*;
import com.shigui.mapper.ClaimRecordMapper;
import com.shigui.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimRecordServiceImpl extends ServiceImpl<ClaimRecordMapper, ClaimRecord> implements ClaimRecordService {
    private final LostFoundPostService lostFoundPostService;
    private final AppUserService appUserService;
    private final AiClaimReviewClient aiClaimReviewClient;
    private final AiClaimReviewProperties properties;

    public ClaimRecordServiceImpl(LostFoundPostService lostFoundPostService, AppUserService appUserService,
                                  AiClaimReviewClient aiClaimReviewClient, AiClaimReviewProperties properties) {
        this.lostFoundPostService = lostFoundPostService;
        this.appUserService = appUserService;
        this.aiClaimReviewClient = aiClaimReviewClient;
        this.properties = properties;
    }

    @Override
    @Transactional
    public ClaimResponse createClaim(Long userId, CreateClaimRequest request) {
        AppUser user = appUserService.getByIdOrThrow(userId);
        if ("BANNED".equals(user.getStatus())) throw new IllegalArgumentException("用户已被封禁");
        LostFoundPost post = lostFoundPostService.getById(request.getPostId());
        if (post == null) throw new IllegalArgumentException("单据不存在");
        if (!"MATCHING".equals(post.getStatus())) throw new IllegalArgumentException("该单据当前不可认领");
        if (post.getUserId().equals(userId)) throw new IllegalArgumentException("不能认领自己发布的单据");

        Long existing = count(new LambdaQueryWrapper<ClaimRecord>()
                .eq(ClaimRecord::getPostId, request.getPostId())
                .eq(ClaimRecord::getClaimantUserId, userId));
        if (existing > 0) throw new IllegalArgumentException("已提交过认领申请");

        ClaimRecord claim = new ClaimRecord();
        claim.setPostId(request.getPostId()); claim.setClaimantUserId(userId);
        claim.setPrivateFeatureAnswer(request.getPrivateFeatureAnswer());
        claim.setStatus("PENDING_ADMIN_REVIEW");

        try {
            AiClaimReviewResult ai = aiClaimReviewClient.reviewClaim(post, request.getPrivateFeatureAnswer());
            claim.setAiDecision(ai.getDecision()); claim.setAiConfidence(ai.getConfidence());
            claim.setAiReason(ai.getReason());
            if ("APPROVE".equals(ai.getDecision()) && ai.getConfidence().compareTo(properties.autoApproveThresholdValue()) >= 0) {
                claim.setStatus("VERIFIED"); claim.setVerifiedAt(LocalDateTime.now());
                post.setStatus("CLAIMING"); lostFoundPostService.updateById(post);
            }
        } catch (Exception ignored) { /* AI unavailable, proceed manual */ }

        save(claim);
        return toResponse(claim, post);
    }

    @Override
    public Page<ClaimResponse> listMine(Long userId, int page, int size) {
        Page<ClaimRecord> entityPage = page(new Page<>(page, size),
                new LambdaQueryWrapper<ClaimRecord>().eq(ClaimRecord::getClaimantUserId, userId)
                        .eq(ClaimRecord::getDeleted, 0).orderByDesc(ClaimRecord::getCreatedAt));
        List<ClaimResponse> responses = entityPage.getRecords().stream()
                .map(r -> toResponse(r, lostFoundPostService.getById(r.getPostId()))).toList();
        Page<ClaimResponse> result = new Page<>(page, size);
        result.setRecords(responses); result.setTotal(entityPage.getTotal());
        return result;
    }

    @Override
    public ClaimResponse getByIdOrThrow(Long claimId) {
        ClaimRecord claim = getById(claimId);
        if (claim == null) throw new IllegalArgumentException("认领记录不存在");
        return toResponse(claim, lostFoundPostService.getById(claim.getPostId()));
    }

    @Override
    @Transactional
    public void confirmReceive(Long claimId, Long userId) {
        ClaimRecord claim = getById(claimId);
        if (claim == null) throw new IllegalArgumentException("认领记录不存在");
        if (!claim.getClaimantUserId().equals(userId)) throw new IllegalArgumentException("无权操作");
        if (!"VERIFIED".equals(claim.getStatus())) throw new IllegalArgumentException("当前状态不可确认收到");
        claim.setStatus("COMPLETED"); claim.setCompletedAt(LocalDateTime.now());
        updateById(claim);
        LostFoundPost post = lostFoundPostService.getById(claim.getPostId());
        if (post != null) { post.setStatus("COMPLETED"); lostFoundPostService.updateById(post); }
    }

    private ClaimResponse toResponse(ClaimRecord claim, LostFoundPost post) {
        ClaimResponse r = new ClaimResponse();
        r.setId(claim.getId()); r.setPostId(claim.getPostId()); r.setStatus(claim.getStatus());
        r.setAiDecision(claim.getAiDecision()); r.setAiConfidence(claim.getAiConfidence());
        r.setAiReason(claim.getAiReason()); r.setAdminReason(claim.getAdminReason());
        r.setCreatedAt(claim.getCreatedAt()); r.setVerifiedAt(claim.getVerifiedAt());
        r.setCompletedAt(claim.getCompletedAt());
        if (post != null) { r.setPostTitle(post.getTitle()); r.setItemName(post.getItemName());
            r.setItemCategory(post.getItemCategory()); r.setCampusArea(post.getCampusArea());
            r.setLocationName(post.getLocationName()); r.setStorageLocation(post.getStorageLocation()); }
        return r;
    }
}
