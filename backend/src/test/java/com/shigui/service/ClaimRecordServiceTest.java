package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.AiClaimReviewResult;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.entity.AppUser;
import com.shigui.entity.ClaimRecord;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.ClaimRecordMapper;
import com.shigui.service.impl.ClaimRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimRecordServiceTest {

    @Mock
    private ClaimRecordMapper claimRecordMapper;

    @Mock
    private LostFoundPostService lostFoundPostService;

    @Mock
    private AppUserService appUserService;

    @Mock
    private AiClaimReviewClient aiClaimReviewClient;

    private ClaimRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        AiClaimReviewProperties properties = new AiClaimReviewProperties();
        properties.setAutoApproveThreshold("0.85");
        properties.setAutoRejectThreshold("0.85");
        service = new ClaimRecordServiceImpl(lostFoundPostService, appUserService, aiClaimReviewClient, properties);
        injectBaseMapper(service, claimRecordMapper);
    }

    @Test
    void createClaim_aiApprove_movesClaimVerifiedAndPostReturning() {
        LostFoundPost post = foundPost();
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(normalUser(2L));
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class))).thenAnswer(inv -> {
            ClaimRecord claim = inv.getArgument(0);
            claim.setId(99L);
            return 1;
        });
        when(claimRecordMapper.updateById(any(ClaimRecord.class))).thenReturn(1);
        when(aiClaimReviewClient.reviewClaim(eq(post), eq("蓝色贴纸"))).thenReturn(aiResult("APPROVE", "0.91"));
        when(lostFoundPostService.updateById(post)).thenReturn(true);

        ClaimResponse response = service.createClaim(2L, createRequest("蓝色贴纸"));

        assertThat(response.getStatus()).isEqualTo("VERIFIED");
        assertThat(response.getVerifiedStorageLocation()).isEqualTo("保卫处前台");
        assertThat(post.getStatus()).isEqualTo("RETURNING");
        verify(lostFoundPostService).updateById(post);
        verify(claimRecordMapper).updateById(any(ClaimRecord.class));
    }

    @Test
    void createClaim_aiReasonUsesSafeSummaryWithoutPrivateDetail() {
        LostFoundPost post = foundPost();
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(normalUser(2L));
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class))).thenAnswer(inv -> {
            ClaimRecord claim = inv.getArgument(0);
            claim.setId(99L);
            return 1;
        });
        when(claimRecordMapper.updateById(any(ClaimRecord.class))).thenReturn(1);
        AiClaimReviewResult ai = aiResult("APPROVE", "0.91");
        ai.setReason("用户回答包含蓝色星星贴纸，确认匹配");
        when(aiClaimReviewClient.reviewClaim(eq(post), eq("蓝色星星贴纸"))).thenReturn(ai);
        when(lostFoundPostService.updateById(post)).thenReturn(true);

        ClaimResponse response = service.createClaim(2L, createRequest("蓝色星星贴纸"));

        assertThat(response.getAiReason()).isEqualTo("私密特征匹配");
        assertThat(response.getAiReason()).doesNotContain("蓝色星星贴纸");
    }

    @Test
    void createClaim_aiNeedsReview_movesClaimPendingAdminReviewAndPostClaiming() {
        LostFoundPost post = foundPost();
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(normalUser(2L));
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class))).thenAnswer(inv -> {
            ClaimRecord claim = inv.getArgument(0);
            claim.setId(99L);
            return 1;
        });
        when(claimRecordMapper.updateById(any(ClaimRecord.class))).thenReturn(1);
        when(aiClaimReviewClient.reviewClaim(eq(post), eq("我的"))).thenReturn(aiResult("NEEDS_REVIEW", "0.50"));
        when(lostFoundPostService.updateById(post)).thenReturn(true);

        ClaimResponse response = service.createClaim(2L, createRequest("我的"));

        assertThat(response.getStatus()).isEqualTo("PENDING_ADMIN_REVIEW");
        assertThat(response.getVerifiedStorageLocation()).isNull();
        assertThat(post.getStatus()).isEqualTo("CLAIMING");
    }

    @Test
    void createClaim_rejectsSelfClaim() {
        LostFoundPost post = foundPost();
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(1L)).thenReturn(normalUser(1L));

        assertThatThrownBy(() -> service.createClaim(1L, createRequest("蓝色贴纸")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能认领自己发布的招领单");
    }

    @Test
    void createClaim_rejectsDeletedPostBecauseLostFoundPostHasNoTableLogic() {
        LostFoundPost post = foundPost();
        post.setDeleted(1);
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(normalUser(2L));

        assertThatThrownBy(() -> service.createClaim(2L, createRequest("蓝色贴纸")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("单据不存在");
    }

    @Test
    void createClaim_translatesDatabaseConflictToReadableError() {
        LostFoundPost post = foundPost();
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(normalUser(2L));
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class)))
                .thenThrow(new DuplicateKeyException("active_claim_post_id"));

        assertThatThrownBy(() -> service.createClaim(2L, createRequest("蓝色贴纸")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("该单据已有进行中的认领申请");
    }

    @Test
    void createClaim_postUpdateFailureIsNotStoredAsAiFailure() {
        LostFoundPost post = foundPost();
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(normalUser(2L));
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class))).thenAnswer(inv -> {
            ClaimRecord claim = inv.getArgument(0);
            claim.setId(99L);
            return 1;
        });
        when(aiClaimReviewClient.reviewClaim(eq(post), eq("蓝色贴纸"))).thenReturn(aiResult("APPROVE", "0.91"));
        when(lostFoundPostService.updateById(post)).thenThrow(new IllegalStateException("post update failed"));

        assertThatThrownBy(() -> service.createClaim(2L, createRequest("蓝色贴纸")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("post update failed");
    }

    @Test
    void listMine_doesNotPopulatePostFieldsFromDeletedPost() {
        ClaimRecord claim = verifiedClaim();
        Page<ClaimRecord> page = new Page<>(1, 10);
        page.setRecords(List.of(claim));
        page.setTotal(1);
        LostFoundPost deletedPost = foundPost();
        deletedPost.setDeleted(1);
        when(claimRecordMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(lostFoundPostService.getById(10L)).thenReturn(deletedPost);

        Page<ClaimResponse> result = service.listMine(2L, 1, 10);
        ClaimResponse response = result.getRecords().get(0);

        assertThat(response.getPostTitle()).isNull();
        assertThat(response.getItemName()).isNull();
        assertThat(response.getVerifiedStorageLocation()).isNull();
        assertThat(response.getStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void listAdminClaims_doesNotPopulatePostFieldsFromDeletedPost() {
        ClaimRecord claim = pendingAdminClaim();
        Page<ClaimRecord> page = new Page<>(1, 10);
        page.setRecords(List.of(claim));
        page.setTotal(1);
        LostFoundPost deletedPost = foundPost();
        deletedPost.setDeleted(1);
        when(claimRecordMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(lostFoundPostService.getById(10L)).thenReturn(deletedPost);

        var result = service.listAdminClaims(1, 10, null);
        var response = result.getRecords().get(0);

        assertThat(response.getPostTitle()).isNull();
        assertThat(response.getStorageLocation()).isNull();
        assertThat(response.getPrivateFeature()).isNull();
        assertThat(response.getStatus()).isEqualTo("PENDING_ADMIN_REVIEW");
    }

    @Test
    void approveByAdmin_verifiesClaimAndReturnsStorageLocation() {
        ClaimRecord claim = pendingAdminClaim();
        LostFoundPost post = foundPost();
        post.setStatus("CLAIMING");
        when(claimRecordMapper.selectById(99L)).thenReturn(claim);
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(claimRecordMapper.update(any(ClaimRecord.class), any())).thenReturn(1);
        when(lostFoundPostService.updateById(post)).thenReturn(true);

        var response = service.approveByAdmin(99L);

        assertThat(response.getStatus()).isEqualTo("VERIFIED");
        assertThat(response.getStorageLocation()).isEqualTo("保卫处前台");
        assertThat(claim.getVerifiedAt()).isNotNull();
        assertThat(post.getStatus()).isEqualTo("RETURNING");
        verify(claimRecordMapper).update(eq(claim), any());
        verify(lostFoundPostService).updateById(post);
    }

    @Test
    void approveByAdmin_concurrentStatusChangeFailsBeforePostUpdate() {
        ClaimRecord claim = pendingAdminClaim();
        LostFoundPost post = foundPost();
        post.setStatus("CLAIMING");
        when(claimRecordMapper.selectById(99L)).thenReturn(claim);
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(claimRecordMapper.update(any(ClaimRecord.class), any())).thenReturn(0);

        assertThatThrownBy(() -> service.approveByAdmin(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前认领申请不可审核");
        verify(lostFoundPostService).getById(10L);
        verifyNoMoreInteractions(lostFoundPostService);
    }

    @Test
    void rejectByAdmin_rejectsClaimAndMovesPostBackToMatching() {
        ClaimRecord claim = pendingAdminClaim();
        LostFoundPost post = foundPost();
        post.setStatus("CLAIMING");
        when(claimRecordMapper.selectById(99L)).thenReturn(claim);
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(claimRecordMapper.update(any(ClaimRecord.class), any())).thenReturn(1);
        when(lostFoundPostService.updateById(post)).thenReturn(true);

        var response = service.rejectByAdmin(99L, "信息不符");

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getAdminReason()).isEqualTo("信息不符");
        assertThat(post.getStatus()).isEqualTo("MATCHING");
        verify(claimRecordMapper).update(eq(claim), any());
        verify(lostFoundPostService).updateById(post);
    }

    @Test
    void confirmReceive_completesVerifiedClaimAndPost() {
        ClaimRecord claim = verifiedClaim();
        LostFoundPost post = foundPost();
        post.setStatus("RETURNING");
        when(claimRecordMapper.selectById(99L)).thenReturn(claim);
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(claimRecordMapper.update(any(ClaimRecord.class), any())).thenReturn(1);
        when(lostFoundPostService.updateById(post)).thenReturn(true);

        ClaimResponse response = service.confirmReceive(2L, 99L);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getVerifiedStorageLocation()).isEqualTo("保卫处前台");
        assertThat(claim.getCompletedAt()).isNotNull();
        assertThat(post.getStatus()).isEqualTo("COMPLETED");
        verify(claimRecordMapper).update(eq(claim), any());
        verify(lostFoundPostService).updateById(post);
    }

    private CreateClaimRequest createRequest(String answer) {
        CreateClaimRequest request = new CreateClaimRequest();
        request.setPostId(10L);
        request.setPrivateFeatureAnswer(answer);
        return request;
    }

    private AiClaimReviewResult aiResult(String decision, String confidence) {
        AiClaimReviewResult result = new AiClaimReviewResult();
        result.setDecision(decision);
        result.setConfidence(new BigDecimal(confidence));
        result.setReason("私密特征匹配");
        return result;
    }

    private LostFoundPost foundPost() {
        LostFoundPost post = new LostFoundPost();
        post.setId(10L);
        post.setUserId(1L);
        post.setPostType("FOUND");
        post.setStatus("MATCHING");
        post.setTitle("捡到校园卡");
        post.setItemName("校园卡");
        post.setItemCategory("证件");
        post.setCampusArea("南校园");
        post.setLocationName("逸夫楼");
        post.setStorageLocation("保卫处前台");
        post.setDeleted(0);
        return post;
    }

    private AppUser normalUser(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setStatus("NORMAL");
        return user;
    }

    private ClaimRecord pendingAdminClaim() {
        ClaimRecord claim = new ClaimRecord();
        claim.setId(99L);
        claim.setPostId(10L);
        claim.setClaimantUserId(2L);
        claim.setPrivateFeatureAnswer("蓝色贴纸");
        claim.setStatus("PENDING_ADMIN_REVIEW");
        claim.setDeleted(0);
        return claim;
    }

    private ClaimRecord verifiedClaim() {
        ClaimRecord claim = pendingAdminClaim();
        claim.setStatus("VERIFIED");
        return claim;
    }

    private void injectBaseMapper(ClaimRecordServiceImpl target, ClaimRecordMapper mapper) {
        try {
            Field field = findField(target.getClass(), "baseMapper");
            field.setAccessible(true);
            field.set(target, mapper);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
