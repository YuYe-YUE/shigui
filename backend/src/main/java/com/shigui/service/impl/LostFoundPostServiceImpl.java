package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.MapPostResponse;
import com.shigui.dto.PostResponse;
import com.shigui.entity.AppUser;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.LostFoundPostMapper;
import com.shigui.service.AppUserService;
import com.shigui.service.LostFoundPostService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LostFoundPostServiceImpl extends ServiceImpl<LostFoundPostMapper, LostFoundPost> implements LostFoundPostService {

    private final AppUserService appUserService;

    public LostFoundPostServiceImpl(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public PostResponse publish(Long userId, CreatePostRequest request) {
        AppUser user = appUserService.getByIdOrThrow(userId);
        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalArgumentException("用户已被封禁，不能发布单据");
        }
        validate(request);

        LostFoundPost post = new LostFoundPost();
        post.setUserId(userId);
        post.setPostType(request.getPostType().trim());
        post.setTitle(request.getTitle().trim());
        post.setItemName(request.getItemName().trim());
        post.setItemCategory(request.getItemCategory().trim());
        post.setDescription(trimToEmpty(request.getDescription()));
        post.setPrivateFeature(trimToEmpty(request.getPrivateFeature()));
        post.setCampusArea(request.getCampusArea().trim());
        post.setLocationName(request.getLocationName().trim());
        post.setLongitude(request.getLongitude());
        post.setLatitude(request.getLatitude());
        post.setStorageLocation(trimToEmpty(request.getStorageLocation()));
        post.setEventTime(request.getEventTime());
        post.setStatus("PENDING_AUDIT");
        post.setPublishedAt(LocalDateTime.now());
        post.setDeleted(0);

        save(post);
        return toResponse(post);
    }

    @Override
    public PostResponse getDetail(Long postId, Long currentUserId) {
        LostFoundPost post = getById(postId);
        if (post == null) {
            throw new IllegalArgumentException("单据不存在: " + postId);
        }
        // 非本人只能看已审核通过的公开单据
        if (!"MATCHING".equals(post.getStatus()) && !post.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("单据不存在: " + postId);
        }
        return toResponse(post);
    }

    @Override
    public Page<PostResponse> listPublic(int page, int size, String postType,
            String itemCategory, String campusArea, String keyword) {
        LambdaQueryWrapper<LostFoundPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LostFoundPost::getStatus, "MATCHING");
        wrapper.eq(LostFoundPost::getDeleted, 0);
        wrapper.eq(postType != null && !postType.isEmpty(), LostFoundPost::getPostType, postType);
        wrapper.eq(itemCategory != null && !itemCategory.isEmpty(), LostFoundPost::getItemCategory, itemCategory);
        wrapper.eq(campusArea != null && !campusArea.isEmpty(), LostFoundPost::getCampusArea, campusArea);
        wrapper.like(keyword != null && !keyword.isEmpty(), LostFoundPost::getTitle, keyword);
        wrapper.orderByDesc(LostFoundPost::getPublishedAt);

        Page<LostFoundPost> entityPage = page(new Page<>(page, size), wrapper);
        List<PostResponse> responses = entityPage.getRecords().stream()
                .map(this::toResponse).toList();

        Page<PostResponse> result = new Page<>(page, size);
        result.setRecords(responses);
        result.setTotal(entityPage.getTotal());
        return result;
    }

    @Override
    public Page<PostResponse> listMine(Long userId, int page, int size, String postType) {
        LambdaQueryWrapper<LostFoundPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LostFoundPost::getUserId, userId);
        wrapper.eq(LostFoundPost::getDeleted, 0);
        wrapper.eq(postType != null && !postType.isEmpty(), LostFoundPost::getPostType, postType);
        wrapper.orderByDesc(LostFoundPost::getPublishedAt);

        Page<LostFoundPost> entityPage = page(new Page<>(page, size), wrapper);
        List<PostResponse> responses = entityPage.getRecords().stream()
                .map(this::toResponse).toList();

        Page<PostResponse> result = new Page<>(page, size);
        result.setRecords(responses);
        result.setTotal(entityPage.getTotal());
        return result;
    }

    @Override
    public List<MapPostResponse> listMapPosts() {
        LambdaQueryWrapper<LostFoundPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                LostFoundPost::getId,
                LostFoundPost::getItemName,
                LostFoundPost::getItemCategory,
                LostFoundPost::getCampusArea,
                LostFoundPost::getLocationName,
                LostFoundPost::getLongitude,
                LostFoundPost::getLatitude,
                LostFoundPost::getEventTime
        );
        wrapper.eq(LostFoundPost::getPostType, "FOUND");
        wrapper.eq(LostFoundPost::getStatus, "MATCHING");
        wrapper.eq(LostFoundPost::getDeleted, 0);
        wrapper.isNotNull(LostFoundPost::getLongitude);
        wrapper.isNotNull(LostFoundPost::getLatitude);
        wrapper.orderByDesc(LostFoundPost::getPublishedAt);

        return list(wrapper).stream().map(this::toMapResponse).toList();
    }

    private void validate(CreatePostRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String postType = trimToEmpty(request.getPostType());
        if (!"LOST".equals(postType) && !"FOUND".equals(postType)) {
            throw new IllegalArgumentException("单据类型必须是 LOST 或 FOUND");
        }
        if (isBlank(request.getTitle())) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (isBlank(request.getItemName())) {
            throw new IllegalArgumentException("物品名称不能为空");
        }
        if (isBlank(request.getItemCategory())) {
            throw new IllegalArgumentException("物品分类不能为空");
        }
        if (isBlank(request.getCampusArea())) {
            throw new IllegalArgumentException("校区不能为空");
        }
        if (isBlank(request.getLocationName())) {
            throw new IllegalArgumentException("地点不能为空");
        }
        if (request.getEventTime() == null) {
            throw new IllegalArgumentException("发生时间不能为空");
        }
    }

    private PostResponse toResponse(LostFoundPost post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setPostType(post.getPostType());
        response.setTitle(post.getTitle());
        response.setItemName(post.getItemName());
        response.setItemCategory(post.getItemCategory());
        response.setDescription(post.getDescription());
        response.setCampusArea(post.getCampusArea());
        response.setLocationName(post.getLocationName());
        response.setEventTime(post.getEventTime());
        response.setPublishedAt(post.getPublishedAt());
        response.setStatus(post.getStatus());
        return response;
    }

    private MapPostResponse toMapResponse(LostFoundPost post) {
        MapPostResponse response = new MapPostResponse();
        response.setId(post.getId());
        response.setItemName(post.getItemName());
        response.setItemCategory(post.getItemCategory());
        response.setCampusArea(post.getCampusArea());
        response.setLocationName(post.getLocationName());
        response.setLongitude(post.getLongitude());
        response.setLatitude(post.getLatitude());
        response.setEventTime(post.getEventTime());
        return response;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
