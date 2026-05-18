package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.AdminClaimResponse;
import com.shigui.dto.AiClaimReviewResult;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.entity.AppUser;
import com.shigui.entity.ClaimRecord;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.ClaimRecordMapper;
import com.shigui.service.AiClaimReviewClient;
import com.shigui.service.AppUserService;
import com.shigui.service.ClaimRecordService;
import com.shigui.service.LostFoundPostService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimRecordServiceImpl extends ServiceImpl<ClaimRecordMapper, ClaimRecord> implements ClaimRecordService {

    private static final String PENDING_AI_REVIEW = "PENDING_AI_REVIEW";
    private static final String PENDING_ADMIN_REVIEW = "PENDING_ADMIN_REVIEW";
    private static final String VERIFIED = "VERIFIED";
    private static final String REJECTED = "REJECTED";
    private static final String COMPLETED = "COMPLETED";

    private final LostFoundPostService lostFoundPostService;
    private final AppUserService appUserService;
    private final AiClaimReviewClient aiClaimReviewClient;
    private final AiClaimReviewProperties properties;

    public ClaimRecordServiceImpl(LostFoundPostService lostFoundPostService,
                                  AppUserService appUserService,
                                  AiClaimReviewClient aiClaimReviewClient,
                                  AiClaimReviewProperties properties) {
        this.lostFoundPostService = lostFoundPostService;
        this.appUserService = appUserService;
        this.aiClaimReviewClient = aiClaimReviewClient;
        this.properties = properties;
    }

    @Override
    @Transactional
    public ClaimResponse createClaim(Long claimantUserId, CreateClaimRequest request) {
        validateCreateRequest(request);
        AppUser user = appUserService.getByIdOrThrow(claimantUserId);
        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalArgumentException("封禁用户不能申请认领");
        }

        LostFoundPost post = lostFoundPostService.getById(request.getPostId());
        validateClaimablePost(post, claimantUserId);
        ensureNoActiveClaim(post.getId());

        ClaimRecord claim = new ClaimRecord();
        claim.setPostId(post.getId());
        claim.setClaimantUserId(claimantUserId);
        claim.setPrivateFeatureAnswer(request.getPrivateFeatureAnswer().trim());
        claim.setStatus(PENDING_AI_REVIEW);
        claim.setDeleted(0);

        try {
            save(claim);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("该单据已有进行中的认领申请", e);
        }

        applyAiReview(post, claim);
        return toResponse(claim, post);
    }

    @Override
    public Page<ClaimResponse> listMine(Long userId, int page, int size) {
        Page<ClaimRecord> entityPage = page(new Page<>(page, size), new LambdaQueryWrapper<ClaimRecord>()
                .eq(ClaimRecord::getClaimantUserId, userId)
                .eq(ClaimRecord::getDeleted, 0)
                .orderByDesc(ClaimRecord::getCreatedAt));

        Page<ClaimResponse> result = new Page<>(page, size);
        result.setRecords(entityPage.getRecords().stream().map(this::toResponse).toList());
        result.setTotal(entityPage.getTotal());
        return result;
    }

    @Override
    @Transactional
    public ClaimResponse confirmReceive(Long userId, Long claimId) {
        ClaimRecord claim = requireClaim(claimId);
        if (!claim.getClaimantUserId().equals(userId)) {
            throw new IllegalArgumentException("只能确认自己的认领申请");
        }
        if (!VERIFIED.equals(claim.getStatus())) {
            throw new IllegalArgumentException("只有已通过的认领申请可以确认收到");
        }

        LostFoundPost post = requirePost(claim.getPostId());
        claim.setStatus(COMPLETED);
        claim.setCompletedAt(LocalDateTime.now());
        updateClaimStatusOrThrow(claim, VERIFIED, "只有已通过的认领申请可以确认收到");

        post.setStatus("COMPLETED");
        updatePostOrThrow(post);
        return toResponse(claim, post);
    }

    @Override
    public Page<AdminClaimResponse> listAdminClaims(int page, int size, String status) {
        LambdaQueryWrapper<ClaimRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClaimRecord::getDeleted, 0);
        wrapper.eq(status != null && !status.isBlank(), ClaimRecord::getStatus, status);
        wrapper.orderByDesc(ClaimRecord::getCreatedAt);

        Page<ClaimRecord> entityPage = page(new Page<>(page, size), wrapper);
        Page<AdminClaimResponse> result = new Page<>(page, size);
        result.setRecords(entityPage.getRecords().stream().map(this::toAdminResponse).toList());
        result.setTotal(entityPage.getTotal());
        return result;
    }

    @Override
    @Transactional
    public AdminClaimResponse approveByAdmin(Long claimId) {
        ClaimRecord claim = requireClaim(claimId);
        ensureAdminReviewable(claim);

        LostFoundPost post = requirePost(claim.getPostId());
        String expectedStatus = claim.getStatus();
        claim.setStatus(VERIFIED);
        claim.setVerifiedAt(LocalDateTime.now());
        updateClaimStatusOrThrow(claim, expectedStatus, "当前认领申请不可审核");

        post.setStatus("RETURNING");
        updatePostOrThrow(post);
        return toAdminResponse(claim, post);
    }

    @Override
    @Transactional
    public AdminClaimResponse rejectByAdmin(Long claimId, String reason) {
        ClaimRecord claim = requireClaim(claimId);
        ensureAdminReviewable(claim);

        LostFoundPost post = requirePost(claim.getPostId());
        String expectedStatus = claim.getStatus();
        claim.setStatus(REJECTED);
        claim.setAdminReason(reason == null ? "" : reason.trim());
        updateClaimStatusOrThrow(claim, expectedStatus, "当前认领申请不可审核");

        post.setStatus("MATCHING");
        updatePostOrThrow(post);
        return toAdminResponse(claim, post);
    }

    private void applyAiReview(LostFoundPost post, ClaimRecord claim) {
        AiClaimReviewResult result;
        try {
            result = aiClaimReviewClient.reviewClaim(post, claim.getPrivateFeatureAnswer());
        } catch (Exception e) {
            claim.setAiDecision("NEEDS_REVIEW");
            claim.setAiConfidence(BigDecimal.ZERO);
            claim.setAiReason("AI 预审失败，等待管理员审核");
            claim.setStatus(PENDING_ADMIN_REVIEW);
            post.setStatus("CLAIMING");
            updatePostOrThrow(post);
            updateClaimOrThrow(claim);
            return;
        }

        claim.setAiDecision(result == null ? "NEEDS_REVIEW" : trimToEmpty(result.getDecision()));
        claim.setAiConfidence(normalize(result == null ? null : result.getConfidence()));

        if ("APPROVE".equals(claim.getAiDecision())
                && claim.getAiConfidence().compareTo(properties.autoApproveThresholdValue()) >= 0) {
            claim.setStatus(VERIFIED);
            claim.setAiReason("私密特征匹配");
            claim.setVerifiedAt(LocalDateTime.now());
            post.setStatus("RETURNING");
            updatePostOrThrow(post);
        } else if ("REJECT".equals(claim.getAiDecision())
                && claim.getAiConfidence().compareTo(properties.autoRejectThresholdValue()) >= 0) {
            claim.setStatus(REJECTED);
            claim.setAiReason("私密特征不匹配");
            post.setStatus("MATCHING");
            updatePostOrThrow(post);
        } else {
            claim.setStatus(PENDING_ADMIN_REVIEW);
            claim.setAiReason("信息不足，需人工复核");
            post.setStatus("CLAIMING");
            updatePostOrThrow(post);
        }
        updateClaimOrThrow(claim);
    }

    private void validateCreateRequest(CreateClaimRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getPostId() == null) {
            throw new IllegalArgumentException("postId 不能为空");
        }
        if (request.getPrivateFeatureAnswer() == null || request.getPrivateFeatureAnswer().isBlank()) {
            throw new IllegalArgumentException("私密特征答案不能为空");
        }
    }

    private void validateClaimablePost(LostFoundPost post, Long claimantUserId) {
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())) {
            throw new IllegalArgumentException("单据不存在");
        }
        if (!"FOUND".equals(post.getPostType())) {
            throw new IllegalArgumentException("只能认领招领单");
        }
        if (!"MATCHING".equals(post.getStatus())) {
            throw new IllegalArgumentException("只能认领匹配中的招领单");
        }
        if (post.getUserId().equals(claimantUserId)) {
            throw new IllegalArgumentException("不能认领自己发布的招领单");
        }
    }

    private void ensureNoActiveClaim(Long postId) {
        long running = count(new LambdaQueryWrapper<ClaimRecord>()
                .eq(ClaimRecord::getPostId, postId)
                .in(ClaimRecord::getStatus, List.of(PENDING_AI_REVIEW, PENDING_ADMIN_REVIEW, VERIFIED)));
        if (running > 0) {
            throw new IllegalArgumentException("该单据已有进行中的认领申请");
        }
    }

    private ClaimRecord requireClaim(Long claimId) {
        ClaimRecord claim = getById(claimId);
        if (claim == null || Integer.valueOf(1).equals(claim.getDeleted())) {
            throw new IllegalArgumentException("认领申请不存在");
        }
        return claim;
    }

    private LostFoundPost requirePost(Long postId) {
        LostFoundPost post = lostFoundPostService.getById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())) {
            throw new IllegalArgumentException("单据不存在");
        }
        return post;
    }

    private void ensureAdminReviewable(ClaimRecord claim) {
        if (!List.of(PENDING_AI_REVIEW, PENDING_ADMIN_REVIEW).contains(claim.getStatus())) {
            throw new IllegalArgumentException("当前认领申请不可审核");
        }
    }

    private void updateClaimStatusOrThrow(ClaimRecord claim, String expectedStatus, String failureMessage) {
        int rows = baseMapper.update(claim, new LambdaUpdateWrapper<ClaimRecord>()
                .eq(ClaimRecord::getId, claim.getId())
                .eq(ClaimRecord::getStatus, expectedStatus)
                .eq(ClaimRecord::getDeleted, 0));
        if (rows != 1) {
            throw new IllegalArgumentException(failureMessage);
        }
    }

    private void updateClaimOrThrow(ClaimRecord claim) {
        if (!updateById(claim)) {
            throw new IllegalStateException("认领申请更新失败");
        }
    }

    private void updatePostOrThrow(LostFoundPost post) {
        if (!lostFoundPostService.updateById(post)) {
            throw new IllegalStateException("单据状态更新失败");
        }
    }

    private ClaimResponse toResponse(ClaimRecord claim) {
        return toResponse(claim, lostFoundPostService.getById(claim.getPostId()));
    }

    private ClaimResponse toResponse(ClaimRecord claim, LostFoundPost post) {
        ClaimResponse response = new ClaimResponse();
        response.setId(claim.getId());
        response.setPostId(claim.getPostId());
        if (isVisiblePost(post)) {
            response.setPostTitle(post.getTitle());
            response.setItemName(post.getItemName());
            response.setItemCategory(post.getItemCategory());
            response.setCampusArea(post.getCampusArea());
            response.setLocationName(post.getLocationName());
            if (List.of(VERIFIED, COMPLETED).contains(claim.getStatus())) {
                response.setVerifiedStorageLocation(post.getStorageLocation());
            }
        }
        response.setStatus(claim.getStatus());
        response.setAiDecision(claim.getAiDecision());
        response.setAiConfidence(claim.getAiConfidence());
        response.setAiReason(claim.getAiReason());
        response.setAdminReason(claim.getAdminReason());
        response.setCreatedAt(claim.getCreatedAt());
        response.setVerifiedAt(claim.getVerifiedAt());
        response.setCompletedAt(claim.getCompletedAt());
        return response;
    }

    private AdminClaimResponse toAdminResponse(ClaimRecord claim) {
        return toAdminResponse(claim, lostFoundPostService.getById(claim.getPostId()));
    }

    private AdminClaimResponse toAdminResponse(ClaimRecord claim, LostFoundPost post) {
        AdminClaimResponse response = new AdminClaimResponse();
        response.setId(claim.getId());
        response.setPostId(claim.getPostId());
        response.setClaimantUserId(claim.getClaimantUserId());
        response.setPrivateFeatureAnswer(claim.getPrivateFeatureAnswer());
        response.setStatus(claim.getStatus());
        response.setAiDecision(claim.getAiDecision());
        response.setAiConfidence(claim.getAiConfidence());
        response.setAiReason(claim.getAiReason());
        response.setAdminReason(claim.getAdminReason());
        response.setCreatedAt(claim.getCreatedAt());
        response.setVerifiedAt(claim.getVerifiedAt());
        response.setCompletedAt(claim.getCompletedAt());
        if (isVisiblePost(post)) {
            response.setPostTitle(post.getTitle());
            response.setItemName(post.getItemName());
            response.setItemCategory(post.getItemCategory());
            response.setCampusArea(post.getCampusArea());
            response.setLocationName(post.getLocationName());
            response.setStorageLocation(post.getStorageLocation());
            response.setPrivateFeature(post.getPrivateFeature());
        }
        return response;
    }

    private boolean isVisiblePost(LostFoundPost post) {
        return post != null && !Integer.valueOf(1).equals(post.getDeleted());
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
