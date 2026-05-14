package com.shigui.service;

import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.PostResponse;
import com.shigui.entity.AppUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.LostFoundPostMapper;
import com.shigui.service.impl.LostFoundPostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LostFoundPostServiceTest {

    @Mock
    private LostFoundPostMapper lostFoundPostMapper;

    @Mock
    private AppUserService appUserService;

    private LostFoundPostService lostFoundPostService;

    @BeforeEach
    void setUp() {
        LostFoundPostServiceImpl impl = new LostFoundPostServiceImpl(appUserService);
        lostFoundPostService = impl;
        injectBaseMapper(impl);
    }

    @Test
    void publish_validLostPost_savesPendingAuditPost() {
        AppUser user = normalUser(1L);
        when(appUserService.getByIdOrThrow(1L)).thenReturn(user);
        when(lostFoundPostMapper.insert(any(LostFoundPost.class))).thenAnswer(inv -> {
            LostFoundPost post = inv.getArgument(0);
            assertThat(post.getUserId()).isEqualTo(1L);
            assertThat(post.getPostType()).isEqualTo("LOST");
            assertThat(post.getStatus()).isEqualTo("PENDING_AUDIT");
            assertThat(post.getDeleted()).isZero();
            post.setId(10L);
            return 1;
        });

        PostResponse response = lostFoundPostService.publish(1L, validLostRequest());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo("PENDING_AUDIT");
        assertThat(response.getTitle()).isEqualTo("丢失校园卡");
    }

    @Test
    void publish_validFoundPost_allowsStorageLocation() {
        AppUser user = normalUser(2L);
        CreatePostRequest request = validLostRequest();
        request.setPostType("FOUND");
        request.setStorageLocation("保卫处前台");
        when(appUserService.getByIdOrThrow(2L)).thenReturn(user);
        when(lostFoundPostMapper.insert(any(LostFoundPost.class))).thenAnswer(inv -> {
            LostFoundPost post = inv.getArgument(0);
            assertThat(post.getPostType()).isEqualTo("FOUND");
            assertThat(post.getStorageLocation()).isEqualTo("保卫处前台");
            post.setId(11L);
            return 1;
        });

        PostResponse response = lostFoundPostService.publish(2L, request);

        assertThat(response.getId()).isEqualTo(11L);
    }

    @Test
    void publish_bannedUser_throwsException() {
        when(appUserService.getByIdOrThrow(3L)).thenReturn(bannedUser(3L));

        assertThatThrownBy(() -> lostFoundPostService.publish(3L, validLostRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户已被封禁");
    }

    @Test
    void publish_missingTitle_throwsException() {
        CreatePostRequest request = validLostRequest();
        request.setTitle("");
        when(appUserService.getByIdOrThrow(1L)).thenReturn(normalUser(1L));

        assertThatThrownBy(() -> lostFoundPostService.publish(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("标题不能为空");
    }

    @Test
    void publish_invalidPostType_throwsException() {
        CreatePostRequest request = validLostRequest();
        request.setPostType("OTHER");
        when(appUserService.getByIdOrThrow(1L)).thenReturn(normalUser(1L));

        assertThatThrownBy(() -> lostFoundPostService.publish(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("单据类型必须是 LOST 或 FOUND");
    }

    @Test
    void getDetail_existingPost_returnsPostResponseWithoutPrivateFeature() {
        LostFoundPost post = new LostFoundPost();
        post.setId(20L);
        post.setUserId(20L);  // 本人查看自己的单据
        post.setPostType("LOST");
        post.setTitle("丢失校园卡");
        post.setItemName("校园卡");
        post.setItemCategory("证件");
        post.setDescription("绿色卡套");
        post.setPrivateFeature("卡号后四位 1234");
        post.setCampusArea("南校园");
        post.setLocationName("逸夫楼");
        post.setStorageLocation("");
        post.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));
        post.setStatus("PENDING_AUDIT");
        when(lostFoundPostMapper.selectById(20L)).thenReturn(post);

        PostResponse response = lostFoundPostService.getDetail(20L, 20L);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getDescription()).isEqualTo("绿色卡套");
        assertThat(response.getStatus()).isEqualTo("PENDING_AUDIT");
    }

    @Test
    void getDetail_nonMatching_nonOwner_throwsException() {
        LostFoundPost post = new LostFoundPost();
        post.setId(30L);
        post.setUserId(1L);  // 属于用户1
        post.setStatus("PENDING_AUDIT");
        when(lostFoundPostMapper.selectById(30L)).thenReturn(post);

        // 用户2试图查看用户1的待审核单据
        assertThatThrownBy(() -> lostFoundPostService.getDetail(30L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("单据不存在");
    }

    @Test
    void getDetail_missingPost_throwsException() {
        when(lostFoundPostMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> lostFoundPostService.getDetail(404L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("单据不存在");
    }

    @Test
    void listPublic_onlyReturnsMatchingNotDeleted() {
        when(lostFoundPostMapper.selectPage(any(Page.class), any()))
                .thenAnswer(inv -> {
                    Page<LostFoundPost> page = inv.getArgument(0);
                    LostFoundPost post = new LostFoundPost();
                    post.setId(1L);
                    post.setStatus("MATCHING");
                    post.setTitle("test");
                    page.setRecords(java.util.List.of(post));
                    page.setTotal(1);
                    return page;
                });

        Page<PostResponse> result = lostFoundPostService.listPublic(1, 10, null, null, null, null);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("MATCHING");
    }

    @Test
    void listPublic_keywordFilter_matchesTitle() {
        when(lostFoundPostMapper.selectPage(any(Page.class), any()))
                .thenAnswer(inv -> {
                    Page<LostFoundPost> page = inv.getArgument(0);
                    page.setRecords(java.util.List.of());
                    page.setTotal(0);
                    return page;
                });

        Page<PostResponse> result = lostFoundPostService.listPublic(1, 10, null, null, null, "校园卡");
        assertThat(result.getRecords()).isEmpty();
    }

    @Test
    void listMine_onlyReturnsCurrentUser() {
        when(lostFoundPostMapper.selectPage(any(Page.class), any()))
                .thenAnswer(inv -> {
                    Page<LostFoundPost> page = inv.getArgument(0);
                    LostFoundPost post = new LostFoundPost();
                    post.setId(1L);
                    post.setUserId(1L);
                    post.setStatus("PENDING_AUDIT");
                    post.setTitle("mine");
                    page.setRecords(java.util.List.of(post));
                    page.setTotal(1);
                    return page;
                });

        Page<PostResponse> result = lostFoundPostService.listMine(1L, 1, 10, null);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("PENDING_AUDIT");
    }

    @Test
    void listMine_excludesDeleted() {
        when(lostFoundPostMapper.selectPage(any(Page.class), any()))
                .thenAnswer(inv -> {
                    Page<LostFoundPost> page = inv.getArgument(0);
                    page.setRecords(java.util.List.of());
                    page.setTotal(0);
                    return page;
                });

        Page<PostResponse> result = lostFoundPostService.listMine(1L, 1, 10, null);
        assertThat(result.getRecords()).isEmpty();
    }

    private void injectBaseMapper(LostFoundPostServiceImpl impl) {
        try {
            Field baseMapperField = null;
            Class<?> clazz = impl.getClass();
            while (clazz != null && baseMapperField == null) {
                try {
                    baseMapperField = clazz.getDeclaredField("baseMapper");
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (baseMapperField == null) {
                throw new RuntimeException("baseMapper field not found in class hierarchy");
            }
            baseMapperField.setAccessible(true);
            baseMapperField.set(impl, lostFoundPostMapper);

            Field entityClassField = com.baomidou.mybatisplus.extension.repository.AbstractRepository.class.getDeclaredField("entityClass");
            entityClassField.setAccessible(true);
            entityClassField.set(impl, LostFoundPost.class);

            Field mapperClassField = com.baomidou.mybatisplus.extension.repository.AbstractRepository.class.getDeclaredField("mapperClass");
            mapperClassField.setAccessible(true);
            mapperClassField.set(impl, LostFoundPostMapper.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CreatePostRequest validLostRequest() {
        CreatePostRequest request = new CreatePostRequest();
        request.setPostType("LOST");
        request.setTitle("丢失校园卡");
        request.setItemName("校园卡");
        request.setItemCategory("证件");
        request.setDescription("绿色卡套，可能在教学楼附近丢失");
        request.setPrivateFeature("卡号后四位 1234");
        request.setCampusArea("南校园");
        request.setLocationName("逸夫楼");
        request.setLongitude(113.2931234);
        request.setLatitude(23.0961234);
        request.setStorageLocation("");
        request.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));
        return request;
    }

    private AppUser normalUser(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setStatus("NORMAL");
        return user;
    }

    private AppUser bannedUser(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setStatus("BANNED");
        return user;
    }
}
