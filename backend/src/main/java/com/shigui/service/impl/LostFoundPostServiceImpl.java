package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.MapPostResponse;
import com.shigui.dto.PostResponse;
import com.shigui.entity.AppUser;
import com.shigui.entity.ClaimRecord;
import com.shigui.entity.LostFoundPost;
import com.shigui.entity.PostImage;
import com.shigui.mapper.ClaimRecordMapper;
import com.shigui.mapper.LostFoundPostMapper;
import com.shigui.mapper.PostImageMapper;
import com.shigui.service.AppUserService;
import com.shigui.service.FileStorageService;
import com.shigui.service.LostFoundPostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 失物招领单据实现：发布审批、公开/个人列表、地图点位，含图片存储关联。
 */
@Service
public class LostFoundPostServiceImpl extends ServiceImpl<LostFoundPostMapper, LostFoundPost> implements LostFoundPostService {
    private static final String POST_IMAGE_PREFIX = "/uploads/posts/";

    private final AppUserService appUserService;
    private final PostImageMapper postImageMapper;
    private final FileStorageService fileStorageService;
    private final ClaimRecordMapper claimRecordMapper;

    public LostFoundPostServiceImpl(
            AppUserService appUserService,
            PostImageMapper postImageMapper,
            FileStorageService fileStorageService,
            ClaimRecordMapper claimRecordMapper) {
        this.appUserService = appUserService;
        this.postImageMapper = postImageMapper;
        this.fileStorageService = fileStorageService;
        this.claimRecordMapper = claimRecordMapper;
    }

    /** 发布单据：封禁校验、参数校验、保存单据和关联图片 */
    @Override
    @Transactional
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
        savePostImages(post.getId(), request.getImageUrls());
        return toResponse(post, request.getImageUrls());
    }

    /** 获取详情：本人、认领通过者、或 MATCHING 状态公开可见 */
    @Override
    public PostResponse getDetail(Long postId, Long currentUserId) {
        LostFoundPost post = getById(postId);
        if (post == null) {
            throw new IllegalArgumentException("单据不存在: " + postId);
        }
        boolean isOwner = post.getUserId().equals(currentUserId);
        boolean hasVerifiedClaim = false;
        if (currentUserId > 0 && !isOwner) {
            Long count = claimRecordMapper.selectCount(new LambdaQueryWrapper<ClaimRecord>()
                    .eq(ClaimRecord::getPostId, postId)
                    .eq(ClaimRecord::getClaimantUserId, currentUserId)
                    .eq(ClaimRecord::getStatus, "VERIFIED")
                    .eq(ClaimRecord::getDeleted, 0));
            hasVerifiedClaim = count != null && count > 0;
        }
        if (!isOwner && !hasVerifiedClaim && !"MATCHING".equals(post.getStatus())) {
            throw new IllegalArgumentException("单据不存在或暂时不可见: " + postId);
        }
        PostResponse response = toResponse(post);
        // FOUND 招领单：认领通过后才能联系对方。LOST 寻物单：非发布者可直接联系
        response.setCanChat(!isOwner && ("LOST".equals(post.getPostType()) || hasVerifiedClaim));
        return response;
    }

    /** 公开列表：按类型/分类/校区/关键字筛选已审核的 MATCHING 单据 */
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
        Map<Long, List<String>> imageUrlsByPostId = loadImageUrlsByPostIds(entityPage.getRecords());
        List<PostResponse> responses = entityPage.getRecords().stream()
                .map(post -> toResponse(post, imageUrlsByPostId.get(post.getId()))).toList();

        Page<PostResponse> result = new Page<>(page, size);
        result.setRecords(responses);
        result.setTotal(entityPage.getTotal());
        return result;
    }

    /** 我的发布记录 */
    @Override
    public Page<PostResponse> listMine(Long userId, int page, int size, String postType) {
        LambdaQueryWrapper<LostFoundPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LostFoundPost::getUserId, userId);
        wrapper.eq(LostFoundPost::getDeleted, 0);
        wrapper.eq(postType != null && !postType.isEmpty(), LostFoundPost::getPostType, postType);
        wrapper.orderByDesc(LostFoundPost::getPublishedAt);

        Page<LostFoundPost> entityPage = page(new Page<>(page, size), wrapper);
        Map<Long, List<String>> imageUrlsByPostId = loadImageUrlsByPostIds(entityPage.getRecords());
        List<PostResponse> responses = entityPage.getRecords().stream()
                .map(post -> toResponse(post, imageUrlsByPostId.get(post.getId()))).toList();

        Page<PostResponse> result = new Page<>(page, size);
        result.setRecords(responses);
        result.setTotal(entityPage.getTotal());
        return result;
    }

    /** 管理员无条件获取单据详情，含私密特征和图片 */
    @Override
    public PostResponse getDetailForAdmin(Long postId) {
        LostFoundPost post = getById(postId);
        if (post == null) throw new IllegalArgumentException("单据不存在: " + postId);
        PostResponse response = toResponse(post);
        response.setPrivateFeature(post.getPrivateFeature());
        response.setUserId(post.getUserId() != null ? post.getUserId().toString() : null);
        return response;
    }

    /** 招领地图点位：返回所有带经纬度的 FOUND 单据 */
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
        List<String> imageUrls = request.getImageUrls();
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        if (imageUrls.size() > 3) {
            throw new IllegalArgumentException("最多上传 3 张图片");
        }
        for (String imageUrl : imageUrls) {
            if (isBlank(imageUrl) || !imageUrl.startsWith(POST_IMAGE_PREFIX)) {
                throw new IllegalArgumentException("图片 URL 必须以 /uploads/posts/ 开头");
            }
            if (!fileStorageService.isStoredPostImage(imageUrl)) {
                throw new IllegalArgumentException("图片不存在或不可用");
            }
        }
    }

    private PostResponse toResponse(LostFoundPost post) {
        return toResponse(post, loadImageUrls(post.getId()));
    }

    private PostResponse toResponse(LostFoundPost post, List<String> imageUrls) {
        List<String> safeImageUrls = imageUrls == null ? Collections.emptyList() : List.copyOf(imageUrls);
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
        response.setPublishedAt(post.getPublishedAt());
        response.setStatus(post.getStatus());
        response.setImageUrls(safeImageUrls);
        response.setCoverImageUrl(safeImageUrls.isEmpty() ? null : safeImageUrls.get(0));
        return response;
    }

    private void savePostImages(Long postId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (int index = 0; index < imageUrls.size(); index++) {
            PostImage postImage = new PostImage();
            postImage.setPostId(postId);
            postImage.setImageUrl(imageUrls.get(index));
            postImage.setSortOrder(index);
            postImage.setDeleted(0);
            postImage.setCreatedAt(LocalDateTime.now());
            postImageMapper.insert(postImage);
        }
    }

    private List<String> loadImageUrls(Long postId) {
        if (postId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<PostImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostImage::getPostId, postId);
        wrapper.eq(PostImage::getDeleted, 0);
        wrapper.orderByAsc(PostImage::getSortOrder, PostImage::getId);
        List<PostImage> postImages = postImageMapper.selectList(wrapper);
        if (postImages == null || postImages.isEmpty()) {
            return Collections.emptyList();
        }
        return postImages.stream()
                .map(PostImage::getImageUrl)
                .toList();
    }

    private Map<Long, List<String>> loadImageUrlsByPostIds(List<LostFoundPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> postIds = posts.stream()
                .map(LostFoundPost::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<PostImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PostImage::getPostId, postIds);
        wrapper.eq(PostImage::getDeleted, 0);
        wrapper.orderByAsc(PostImage::getSortOrder, PostImage::getId);
        return postImageMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(
                        PostImage::getPostId,
                        LinkedHashMap::new,
                        Collectors.mapping(PostImage::getImageUrl, Collectors.toList())
                ));
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
