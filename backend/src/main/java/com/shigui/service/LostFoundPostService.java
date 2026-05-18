package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.MapPostResponse;
import com.shigui.dto.PostResponse;
import com.shigui.entity.LostFoundPost;

import java.util.List;

/**
 * 失物招领单据服务：发布、查询、列表、地图点位的核心业务接口。
 */
public interface LostFoundPostService extends IService<LostFoundPost> {
    /** 发布新的失物/招领单据，自动进入待审核状态 */
    PostResponse publish(Long userId, CreatePostRequest request);
    /** 获取单据详情，非本人只能查看已审核通过的公开单据 */
    PostResponse getDetail(Long postId, Long currentUserId);

    /** 公开列表：按类型、分类、校区、关键字分页查询审核中的单据 */
    Page<PostResponse> listPublic(int page, int size, String postType,
            String itemCategory, String campusArea, String keyword);

    /** 我的发布记录 */
    Page<PostResponse> listMine(Long userId, int page, int size, String postType);

    /** 招领地图点位（带经纬度的 FOUND 单） */
    List<MapPostResponse> listMapPosts();
}
