package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.MapPostResponse;
import com.shigui.dto.PostResponse;
import com.shigui.entity.LostFoundPost;

import java.util.List;

public interface LostFoundPostService extends IService<LostFoundPost> {
    PostResponse publish(Long userId, CreatePostRequest request);
    PostResponse getDetail(Long postId, Long currentUserId);

    Page<PostResponse> listPublic(int page, int size, String postType,
            String itemCategory, String campusArea, String keyword);

    Page<PostResponse> listMine(Long userId, int page, int size, String postType);

    List<MapPostResponse> listMapPosts();
}
