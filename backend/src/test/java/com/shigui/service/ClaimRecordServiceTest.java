package com.shigui.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
    void createClaim_aiApprove_returnsVerifiedAndMovesPostToReturning() {
        LostFoundPost post = foundPost();
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user(2L, "NORMAL"));
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class))).thenAnswer(inv -> {
            ClaimRecord claim = inv.getArgument(0);
            claim.setId(99L);
            return 1;
        });
        when(claimRecordMapper.updateById(any(ClaimRecord.class))).thenReturn(1);
        when(lostFoundPostService.updateById(post)).thenReturn(true);
        when(aiClaimReviewClient.reviewClaim(eq(post), eq("蓝色星星贴纸"))).thenReturn(ai("APPROVE", "0.91", "蓝色星星贴纸完全一致"));

        ClaimResponse response = service.createClaim(2L, request("蓝色星星贴纸"));

        assertThat(response.getStatus()).isEqualTo("VERIFIED");
        assertThat(response.getVerifiedStorageLocation()).isEqualTo("保卫处前台");
        assertThat(response.getAiReason()).isEqualTo("私密特征匹配");
        assertThat(response.getAiReason()).doesNotContain("蓝色星星贴纸");
        assertThat(post.getStatus()).isEqualTo("RETURNING");
        verify(lostFoundPostService).updateById(post);
    }

    @Test
    void createClaim_needsReviewDoesNotExposeStorageLocation() {
        LostFoundPost post = foundPost();
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user(2L, "NORMAL"));
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class))).thenAnswer(inv -> {
            ClaimRecord claim = inv.getArgument(0);
            claim.setId(99L);
            return 1;
        });
        when(claimRecordMapper.updateById(any(ClaimRecord.class))).thenReturn(1);
        when(lostFoundPostService.updateById(post)).thenReturn(true);
        when(aiClaimReviewClient.reviewClaim(eq(post), eq("不确定"))).thenReturn(ai("NEEDS_REVIEW", "0.50", "答案含糊"));

        ClaimResponse response = service.createClaim(2L, request("不确定"));

        assertThat(response.getStatus()).isEqualTo("PENDING_ADMIN_REVIEW");
        assertThat(response.getVerifiedStorageLocation()).isNull();
        assertThat(post.getStatus()).isEqualTo("CLAIMING");
    }

    @Test
    void createClaim_duplicateActiveClaimReturnsReadableError() {
        LostFoundPost post = foundPost();
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user(2L, "NORMAL"));
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class))).thenThrow(new DuplicateKeyException("uk_active_claim_post"));

        assertThatThrownBy(() -> service.createClaim(2L, request("蓝色星星贴纸")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("进行中的认领申请");
    }

    @Test
    void adminApproveAndConfirmReceiveFollowReturningThenCompletedFlow() {
        ClaimRecord claim = claim("PENDING_ADMIN_REVIEW");
        LostFoundPost post = foundPost();
        post.setStatus("CLAIMING");
        when(claimRecordMapper.selectById(99L)).thenReturn(claim);
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(claimRecordMapper.update(any(ClaimRecord.class), any(Wrapper.class))).thenReturn(1);
        when(lostFoundPostService.updateById(post)).thenReturn(true);

        var adminResponse = service.approveByAdmin(99L);

        assertThat(adminResponse.getStatus()).isEqualTo("VERIFIED");
        assertThat(post.getStatus()).isEqualTo("RETURNING");

        ClaimRecord verified = claim("VERIFIED");
        when(claimRecordMapper.selectById(99L)).thenReturn(verified);
        ClaimResponse completed = service.confirmReceive(2L, 99L);

        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(post.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void listMine_doesNotPopulateDeletedPostFields() {
        ClaimRecord claim = claim("VERIFIED");
        Page<ClaimRecord> entityPage = new Page<>(1, 10);
        entityPage.setRecords(List.of(claim));
        entityPage.setTotal(1);
        LostFoundPost deletedPost = foundPost();
        deletedPost.setDeleted(1);
        when(claimRecordMapper.selectPage(any(), any())).thenReturn(entityPage);
        when(lostFoundPostService.getById(10L)).thenReturn(deletedPost);

        Page<ClaimResponse> response = service.listMine(2L, 1, 10);

        assertThat(response.getRecords()).hasSize(1);
        assertThat(response.getRecords().get(0).getPostTitle()).isNull();
        assertThat(response.getRecords().get(0).getVerifiedStorageLocation()).isNull();
    }

    private CreateClaimRequest request(String answer) {
        CreateClaimRequest request = new CreateClaimRequest();
        request.setPostId(10L);
        request.setPrivateFeatureAnswer(answer);
        return request;
    }

    private AiClaimReviewResult ai(String decision, String confidence, String reason) {
        AiClaimReviewResult result = new AiClaimReviewResult();
        result.setDecision(decision);
        result.setConfidence(new BigDecimal(confidence));
        result.setReason(reason);
        return result;
    }

    private LostFoundPost foundPost() {
        LostFoundPost post = new LostFoundPost();
        post.setId(10L);
        post.setUserId(1L);
        post.setPostType("FOUND");
        post.setTitle("捡到校园卡");
        post.setItemName("校园卡");
        post.setItemCategory("证件");
        post.setCampusArea("南校园");
        post.setLocationName("逸夫楼");
        post.setStorageLocation("保卫处前台");
        post.setStatus("MATCHING");
        post.setDeleted(0);
        return post;
    }

    private ClaimRecord claim(String status) {
        ClaimRecord claim = new ClaimRecord();
        claim.setId(99L);
        claim.setPostId(10L);
        claim.setClaimantUserId(2L);
        claim.setStatus(status);
        claim.setDeleted(0);
        return claim;
    }

    private AppUser user(Long id, String status) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setStatus(status);
        return user;
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
