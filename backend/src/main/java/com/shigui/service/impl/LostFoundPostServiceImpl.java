package com.shigui.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.PostResponse;
import com.shigui.entity.AppUser;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.LostFoundPostMapper;
import com.shigui.service.AppUserService;
import com.shigui.service.LostFoundPostService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
    public PostResponse getDetail(Long postId) {
        LostFoundPost post = getById(postId);
        if (post == null) {
            throw new IllegalArgumentException("单据不存在: " + postId);
        }
        return toResponse(post);
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
        response.setStorageLocation(post.getStorageLocation());
        response.setEventTime(post.getEventTime());
        response.setStatus(post.getStatus());
        return response;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
