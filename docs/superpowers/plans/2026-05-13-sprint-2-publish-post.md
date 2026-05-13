# Sprint 2: 发布丢失/拾捡单 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现发布丢失/拾捡单的全栈闭环：已登录小程序用户可以提交 `LOST` 或 `FOUND` 单据，新单据统一进入 `PENDING_AUDIT`，等待后续管理端审核。

**Architecture:** 后端按 `LostFoundPostController -> LostFoundPostService -> LostFoundPostMapper` 分层实现参数校验、封禁校验、持久化和单条详情读取。小程序新增“发布”tab、发布类型选择页和发布表单页，提交时携带当前 `satoken` 请求 `/api/posts`。S2 只提供发布和单条详情能力，不实现列表筛选、审核通过、图片上传或智能匹配。

**Tech Stack:** Spring Boot 3.5.14, MyBatis-Plus 3.5.16, Sa-Token 1.45.0, Java 21, Maven, 原生微信小程序。

---

## 文件结构

- Create: `backend/src/main/java/com/shigui/entity/LostFoundPost.java`  
  职责：`lost_found_post` 表对应的 MyBatis-Plus 实体。
- Create: `backend/src/main/java/com/shigui/mapper/LostFoundPostMapper.java`  
  职责：通过 `BaseMapper` 提供数据库基础增删改查。
- Create: `backend/src/main/java/com/shigui/dto/CreatePostRequest.java`  
  职责：`POST /api/posts` 的请求体 DTO。
- Create: `backend/src/main/java/com/shigui/dto/PostResponse.java`  
  职责：发布成功和单条详情共用的响应 DTO，不返回 `privateFeature`。
- Create: `backend/src/main/java/com/shigui/service/LostFoundPostService.java`  
  职责：发布单据用例的 Service 接口。
- Create: `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`  
  职责：参数校验、封禁用户拦截、默认 `PENDING_AUDIT` 状态、数据库保存和单条详情读取。
- Create: `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`  
  职责：需要登录的发布接口入口。
- Create: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`  
  职责：发布业务逻辑的 Service 层 TDD 测试。
- Create: `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java`  
  职责：发布接口的 HTTP 契约测试。
- Modify: `miniapp/app.json`  
  职责：注册发布页面和发布 tab。
- Create: `miniapp/pages/publish/publish.{json,wxml,wxss,js}`  
  职责：进入表单前选择寻物或招领类型。
- Create: `miniapp/pages/publish-form/publish-form.{json,wxml,wxss,js}`  
  职责：收集表单字段并调用后端发布接口。

---

### Task 1: 后端 Entity、Mapper 与 DTO

**Files:**
- Create: `backend/src/main/java/com/shigui/entity/LostFoundPost.java`
- Create: `backend/src/main/java/com/shigui/mapper/LostFoundPostMapper.java`
- Create: `backend/src/main/java/com/shigui/dto/CreatePostRequest.java`
- Create: `backend/src/main/java/com/shigui/dto/PostResponse.java`
- Test: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`

- [ ] **Step 1: 编写会失败的编译契约测试**

创建 `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`:

```java
package com.shigui.service;

import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.PostResponse;
import com.shigui.entity.AppUser;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.LostFoundPostMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LostFoundPostServiceTest {

    @Test
    void compileContract_lostFoundPostAndDtosExposeSprint2Fields() {
        CreatePostRequest request = new CreatePostRequest();
        request.setPostType("LOST");
        request.setTitle("丢失校园卡");
        request.setItemName("校园卡");
        request.setItemCategory("证件");
        request.setDescription("绿色卡套");
        request.setPrivateFeature("卡号后四位 1234");
        request.setCampusArea("南校园");
        request.setLocationName("逸夫楼");
        request.setLongitude(113.2931234);
        request.setLatitude(23.0961234);
        request.setStorageLocation("");
        request.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));

        LostFoundPost post = new LostFoundPost();
        post.setPostType(request.getPostType());
        post.setTitle(request.getTitle());

        PostResponse response = new PostResponse();
        response.setId(1L);
        response.setStatus("PENDING_AUDIT");

        assertThat(post.getPostType()).isEqualTo("LOST");
        assertThat(response.getStatus()).isEqualTo("PENDING_AUDIT");
        assertThat(LostFoundPostMapper.class).isInterface();
        assertThat(AppUser.class).isNotNull();
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest
```

预期：FAIL，测试编译失败，因为 `CreatePostRequest`、`PostResponse`、`LostFoundPost`、`LostFoundPostMapper` 还不存在。

- [ ] **Step 3: 创建 `LostFoundPost` 实体**

创建 `backend/src/main/java/com/shigui/entity/LostFoundPost.java`:

```java
package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lost_found_post")
public class LostFoundPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String postType;
    private String title;
    private String itemName;
    private String itemCategory;
    private String description;
    private String privateFeature;
    private String campusArea;
    private String locationName;
    private Double longitude;
    private Double latitude;
    private String storageLocation;
    private LocalDateTime eventTime;
    private String status;
    private LocalDateTime publishedAt;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 创建 `LostFoundPostMapper`**

创建 `backend/src/main/java/com/shigui/mapper/LostFoundPostMapper.java`:

```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.LostFoundPost;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LostFoundPostMapper extends BaseMapper<LostFoundPost> {
    // 继承 BaseMapper 后已经具备 Sprint 2 需要的基础增删改查能力。
}
```

- [ ] **Step 5: 创建请求 DTO**

创建 `backend/src/main/java/com/shigui/dto/CreatePostRequest.java`:

```java
package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreatePostRequest {
    private String postType;
    private String title;
    private String itemName;
    private String itemCategory;
    private String description;
    private String privateFeature;
    private String campusArea;
    private String locationName;
    private Double longitude;
    private Double latitude;
    private String storageLocation;
    private LocalDateTime eventTime;
}
```

- [ ] **Step 6: 创建响应 DTO**

创建 `backend/src/main/java/com/shigui/dto/PostResponse.java`:

```java
package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResponse {
    private Long id;
    private String postType;
    private String title;
    private String itemName;
    private String itemCategory;
    private String description;
    private String campusArea;
    private String locationName;
    private String storageLocation;
    private LocalDateTime eventTime;
    private String status;
}
```

- [ ] **Step 7: 运行测试，确认通过**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest
```

预期：PASS，1 个测试通过。

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/shigui/entity/LostFoundPost.java \
        backend/src/main/java/com/shigui/mapper/LostFoundPostMapper.java \
        backend/src/main/java/com/shigui/dto/CreatePostRequest.java \
        backend/src/main/java/com/shigui/dto/PostResponse.java \
        backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java
git commit -m "feat: add lost found post entity and DTOs"
```

---

### Task 2: 发布 Service 与参数校验

**Files:**
- Modify: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`
- Create: `backend/src/main/java/com/shigui/service/LostFoundPostService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`

- [ ] **Step 1: 用行为测试替换 Service 测试**

将 `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java` 替换为:

```java
package com.shigui.service;

import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.PostResponse;
import com.shigui.entity.AppUser;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

        PostResponse response = lostFoundPostService.getDetail(20L);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getDescription()).isEqualTo("绿色卡套");
        assertThat(response.getStatus()).isEqualTo("PENDING_AUDIT");
    }

    @Test
    void getDetail_missingPost_throwsException() {
        when(lostFoundPostMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> lostFoundPostService.getDetail(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("单据不存在");
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
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest
```

预期：FAIL，编译失败，因为 `LostFoundPostService` 和 `LostFoundPostServiceImpl` 还不存在。

- [ ] **Step 3: 创建 Service 接口**

创建 `backend/src/main/java/com/shigui/service/LostFoundPostService.java`:

```java
package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.PostResponse;
import com.shigui.entity.LostFoundPost;

public interface LostFoundPostService extends IService<LostFoundPost> {
    /**
     * 发布失物或招领单据。新单据统一进入 PENDING_AUDIT，等待管理端审核。
     */
    PostResponse publish(Long userId, CreatePostRequest request);

    /**
     * 查看单条单据详情。S2 只提供单条详情，不提供列表筛选。
     */
    PostResponse getDetail(Long postId);
}
```

- [ ] **Step 4: 创建 Service 实现**

创建 `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`:

```java
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
```

- [ ] **Step 5: 运行测试，确认通过**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest
```

预期：PASS，7 个测试通过。

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/shigui/service/LostFoundPostService.java \
        backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java \
        backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java
git commit -m "feat: publish lost found posts with validation"
```

---

### Task 3: 发布 Controller API

**Files:**
- Create: `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`
- Create: `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java`

- [ ] **Step 1: 编写 Controller 测试**

创建 `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java`:

```java
package com.shigui.controller;

import com.shigui.dto.PostResponse;
import com.shigui.service.AppUserService;
import com.shigui.service.LostFoundPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LostFoundPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LostFoundPostService lostFoundPostService;

    @MockitoBean
    private AppUserService appUserService;

    @Test
    void publish_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publish_loggedIn_returnsPostResponse() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(10L);
        response.setPostType("LOST");
        response.setTitle("丢失校园卡");
        response.setStatus("PENDING_AUDIT");
        response.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        when(lostFoundPostService.publish(anyLong(), any(CreatePostRequest.class))).thenReturn(response);

        String token = loginAndGetToken();

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("satoken", token)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING_AUDIT"));
    }

    @Test
    void getDetail_loggedIn_returnsPostResponse() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(10L);
        response.setPostType("LOST");
        response.setTitle("丢失校园卡");
        response.setStatus("PENDING_AUDIT");
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        when(lostFoundPostService.getDetail(10L)).thenReturn(response);

        String token = loginAndGetToken();

        mockMvc.perform(get("/api/posts/10")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("丢失校园卡"));
    }

    @Test
    void getDetail_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/posts/10"))
                .andExpect(status().isUnauthorized());
    }

    private String validJson() {
        return """
                {
                  "postType": "LOST",
                  "title": "丢失校园卡",
                  "itemName": "校园卡",
                  "itemCategory": "证件",
                  "description": "绿色卡套",
                  "privateFeature": "卡号后四位 1234",
                  "campusArea": "南校园",
                  "locationName": "逸夫楼",
                  "longitude": 113.2931234,
                  "latitude": 23.0961234,
                  "storageLocation": "",
                  "eventTime": "2026-05-13T09:30:00"
                }
                """;
    }

    private String loginAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openid\":\"controller_publish_user\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return body.replaceAll(".*\\\"data\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostControllerTest
```

预期：FAIL，因为 `LostFoundPostController` 还不存在。

- [ ] **Step 3: 创建 Controller**

创建 `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`:

```java
package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.PostResponse;
import com.shigui.service.LostFoundPostService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class LostFoundPostController {

    private final LostFoundPostService lostFoundPostService;

    public LostFoundPostController(LostFoundPostService lostFoundPostService) {
        this.lostFoundPostService = lostFoundPostService;
    }

    /**
     * 发布新单据。新发布内容先进入 PENDING_AUDIT，由 Sprint 4 管理端审核通过后进入 MATCHING。
     */
    @PostMapping
    public Result<PostResponse> publish(@RequestBody CreatePostRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(lostFoundPostService.publish(userId, request));
    }

    /**
     * 查看单条单据详情，用于发布后验证和后续详情页复用。
     */
    @GetMapping("/{id}")
    public Result<PostResponse> detail(@PathVariable Long id) {
        return Result.ok(lostFoundPostService.getDetail(id));
    }
}
```

- [ ] **Step 4: 检查 Controller 测试边界**
检查这四个测试:
- `publish_notLoggedIn_returns401` 验证真实 Sa-Token 拦截器会拦截未登录 HTTP 请求。
- `publish_loggedIn_returnsPostResponse` 先调用 `/api/user/wx-login` 获取真实 token，再带 `satoken` 请求 `POST /api/posts`。
- `getDetail_loggedIn_returnsPostResponse` 验证登录后可以请求 `GET /api/posts/{id}`。
- `getDetail_notLoggedIn_returns401` 验证未登录不能查看详情。

如果测试文件和 Step 1 一致，这一步不需要改代码。

- [ ] **Step 5: 运行 Controller 测试，确认通过**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostControllerTest
```

预期：PASS，4 个测试通过。

- [ ] **Step 6: 运行全部后端测试**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test
```

预期：PASS，原有 12 个测试加上 Sprint 2 新增测试全部通过。

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/shigui/controller/LostFoundPostController.java \
        backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java
git commit -m "feat: expose publish post API"
```

---

### Task 4: 小程序发布页面与导航

**Files:**
- Modify: `miniapp/app.json`
- Create: `miniapp/pages/publish/publish.js`
- Create: `miniapp/pages/publish/publish.json`
- Create: `miniapp/pages/publish/publish.wxml`
- Create: `miniapp/pages/publish/publish.wxss`
- Create: `miniapp/pages/publish-form/publish-form.js`
- Create: `miniapp/pages/publish-form/publish-form.json`
- Create: `miniapp/pages/publish-form/publish-form.wxml`
- Create: `miniapp/pages/publish-form/publish-form.wxss`

- [ ] **Step 1: 在 app.json 中注册页面和 tab**

修改 `miniapp/app.json`:

```json
{
  "pages": [
    "pages/index/index",
    "pages/publish/publish",
    "pages/publish-form/publish-form",
    "pages/mine/mine"
  ],
  "window": {
    "navigationBarBackgroundColor": "#00573D",
    "navigationBarTitleText": "拾归",
    "navigationBarTextStyle": "white",
    "backgroundColor": "#f0f2f0"
  },
  "tabBar": {
    "color": "#999",
    "selectedColor": "#00573D",
    "backgroundColor": "#fff",
    "borderStyle": "black",
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "首页"
      },
      {
        "pagePath": "pages/publish/publish",
        "text": "发布"
      },
      {
        "pagePath": "pages/mine/mine",
        "text": "我的"
      }
    ]
  }
}
```

- [ ] **Step 2: 创建发布类型选择页脚本**

创建 `miniapp/pages/publish/publish.js`:

```javascript
Page({
  chooseType(event) {
    const type = event.currentTarget.dataset.type
    wx.navigateTo({
      url: `/pages/publish-form/publish-form?type=${type}`
    })
  }
})
```

- [ ] **Step 3: 创建发布类型选择页配置**

创建 `miniapp/pages/publish/publish.json`:

```json
{
  "navigationBarTitleText": "发布"
}
```

- [ ] **Step 4: 创建发布类型选择页结构**

创建 `miniapp/pages/publish/publish.wxml`:

```xml
<view class="page">
  <view class="title">你想发布什么？</view>
  <view class="subtitle">选择类型后填写物品信息，提交后等待管理员审核。</view>

  <view class="type-card lost" data-type="LOST" bindtap="chooseType">
    <view class="type-title">我丢了东西</view>
    <view class="type-desc">发布寻物启事，让同学帮你找回。</view>
  </view>

  <view class="type-card found" data-type="FOUND" bindtap="chooseType">
    <view class="type-title">我捡到东西</view>
    <view class="type-desc">发布招领信息，等待失主认领。</view>
  </view>
</view>
```

- [ ] **Step 5: 创建发布类型选择页样式**

创建 `miniapp/pages/publish/publish.wxss`:

```css
.page {
  min-height: 100vh;
  padding: 80rpx 40rpx;
  background: var(--bg-color);
  box-sizing: border-box;
}

.title {
  font-size: 44rpx;
  font-weight: 700;
  color: var(--text-main);
}

.subtitle {
  margin-top: 16rpx;
  margin-bottom: 48rpx;
  color: var(--text-second);
  font-size: 28rpx;
  line-height: 1.6;
}

.type-card {
  padding: 44rpx 36rpx;
  margin-bottom: 28rpx;
  border-radius: 24rpx;
  background: var(--white);
  box-shadow: var(--shadow);
}

.type-card.lost {
  border-left: 12rpx solid var(--lost-color);
}

.type-card.found {
  border-left: 12rpx solid var(--found-color);
}

.type-title {
  font-size: 34rpx;
  font-weight: 700;
}

.type-desc {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--text-second);
}
```

- [ ] **Step 6: 创建发布表单页脚本**

创建 `miniapp/pages/publish-form/publish-form.js`:

```javascript
const app = getApp()

Page({
  data: {
    postType: 'LOST',
    title: '',
    itemName: '',
    itemCategory: '',
    description: '',
    privateFeature: '',
    campusArea: '',
    locationName: '',
    storageLocation: '',
    eventTime: '',
    submitting: false
  },

  onLoad(options) {
    const type = options.type === 'FOUND' ? 'FOUND' : 'LOST'
    this.setData({ postType: type })
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ [field]: event.detail.value })
  },

  onDateChange(event) {
    this.setData({ eventTime: event.detail.value })
  },

  submitPost() {
    const token = app.globalData.token
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      wx.switchTab({ url: '/pages/mine/mine' })
      return
    }
    const error = this.validate()
    if (error) {
      wx.showToast({ title: error, icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    wx.request({
      url: `${app.globalData.baseUrl}/api/posts`,
      method: 'POST',
      header: { satoken: token },
      data: this.buildPayload(),
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({ title: '提交成功，等待审核', icon: 'success' })
          setTimeout(() => wx.switchTab({ url: '/pages/mine/mine' }), 800)
        } else {
          wx.showToast({ title: res.data.message || '提交失败', icon: 'none' })
        }
      },
      fail: () => wx.showToast({ title: '网络错误', icon: 'none' }),
      complete: () => this.setData({ submitting: false })
    })
  },

  validate() {
    if (!this.data.title.trim()) return '请填写标题'
    if (!this.data.itemName.trim()) return '请填写物品名称'
    if (!this.data.itemCategory.trim()) return '请填写物品分类'
    if (!this.data.campusArea.trim()) return '请填写校区'
    if (!this.data.locationName.trim()) return '请填写地点'
    if (!this.data.eventTime.trim()) return '请选择时间'
    return ''
  },

  buildPayload() {
    return {
      postType: this.data.postType,
      title: this.data.title,
      itemName: this.data.itemName,
      itemCategory: this.data.itemCategory,
      description: this.data.description,
      privateFeature: this.data.privateFeature,
      campusArea: this.data.campusArea,
      locationName: this.data.locationName,
      storageLocation: this.data.postType === 'FOUND' ? this.data.storageLocation : '',
      eventTime: `${this.data.eventTime}T00:00:00`
    }
  }
})
```

- [ ] **Step 7: 创建发布表单页配置**

创建 `miniapp/pages/publish-form/publish-form.json`:

```json
{
  "navigationBarTitleText": "填写信息"
}
```

- [ ] **Step 8: 创建发布表单页结构**

创建 `miniapp/pages/publish-form/publish-form.wxml`:

```xml
<view class="page">
  <view class="form-title">{{postType === 'LOST' ? '发布寻物' : '发布招领'}}</view>

  <view class="field">
    <text class="label">标题</text>
    <input value="{{title}}" data-field="title" bindinput="onInput" placeholder="例如：丢失校园卡" />
  </view>

  <view class="field">
    <text class="label">物品名称</text>
    <input value="{{itemName}}" data-field="itemName" bindinput="onInput" placeholder="例如：校园卡" />
  </view>

  <view class="field">
    <text class="label">物品分类</text>
    <input value="{{itemCategory}}" data-field="itemCategory" bindinput="onInput" placeholder="例如：证件 / 数码 / 衣物" />
  </view>

  <view class="field">
    <text class="label">校区</text>
    <input value="{{campusArea}}" data-field="campusArea" bindinput="onInput" placeholder="例如：南校园" />
  </view>

  <view class="field">
    <text class="label">地点</text>
    <input value="{{locationName}}" data-field="locationName" bindinput="onInput" placeholder="例如：逸夫楼" />
  </view>

  <view class="field">
    <text class="label">发生日期</text>
    <picker mode="date" value="{{eventTime}}" bindchange="onDateChange">
      <view class="picker-value {{eventTime ? '' : 'placeholder'}}">{{eventTime || '请选择发生日期'}}</view>
    </picker>
  </view>

  <view class="field">
    <text class="label">描述</text>
    <textarea value="{{description}}" data-field="description" bindinput="onInput" placeholder="描述物品外观、可能位置等信息" />
  </view>

  <view class="field">
    <text class="label">私密特征</text>
    <textarea value="{{privateFeature}}" data-field="privateFeature" bindinput="onInput" placeholder="用于认领核验，不会在地图公开接口返回" />
  </view>

  <view class="field" wx:if="{{postType === 'FOUND'}}">
    <text class="label">暂存地点</text>
    <input value="{{storageLocation}}" data-field="storageLocation" bindinput="onInput" placeholder="例如：保卫处前台" />
  </view>

  <button class="submit" loading="{{submitting}}" disabled="{{submitting}}" bindtap="submitPost">提交审核</button>
</view>
```

- [ ] **Step 9: 创建发布表单页样式**

创建 `miniapp/pages/publish-form/publish-form.wxss`:

```css
.page {
  min-height: 100vh;
  padding: 48rpx 32rpx 80rpx;
  background: var(--bg-color);
  box-sizing: border-box;
}

.form-title {
  margin-bottom: 32rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: var(--text-main);
}

.field {
  margin-bottom: 24rpx;
  padding: 24rpx;
  background: var(--white);
  border-radius: 20rpx;
  box-shadow: var(--shadow);
}

.label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 26rpx;
  color: var(--text-second);
}

input,
textarea {
  width: 100%;
  min-height: 48rpx;
  color: var(--text-main);
  font-size: 30rpx;
}

textarea {
  min-height: 140rpx;
}

.picker-value {
  min-height: 48rpx;
  color: var(--text-main);
  font-size: 30rpx;
}

.picker-value.placeholder {
  color: var(--text-light);
}

.submit {
  margin-top: 40rpx;
  border-radius: 44rpx;
  background: #00573D;
  color: #fff;
  font-size: 30rpx;
}
```

- [ ] **Step 10: 在微信开发者工具中验证**

用微信开发者工具打开 `/Users/cyrene/Dev/shigui/miniapp`。

预期：
- tabBar 显示：首页 / 发布 / 我的。
- 点击“发布”后展示 LOST 和 FOUND 两个选择。
- 选择任一类型后进入表单页。
- 必填字段为空时显示 toast 校验提示。
- 未登录提交时跳转到“我的”页面。

- [ ] **Step 11: Commit**

```bash
git add miniapp/app.json \
        miniapp/pages/publish \
        miniapp/pages/publish-form
git commit -m "feat: add miniapp publish flow"
```

---

### Task 5: 端到端冒烟验证

**Files:**
- 正常情况下不需要新增或修改代码文件；如果前面任务暴露契约不一致，再做最小修正。

- [ ] **Step 1: 运行后端测试**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test
```

预期：BUILD SUCCESS。

- [ ] **Step 2: 运行管理端构建，确认共享改动没有破坏仓库**

```bash
cd /Users/cyrene/Dev/shigui/admin-web
npm run build
```

预期：构建成功。当前 Element Plus 打包可能出现 Vite chunk 体积警告，可以接受。

- [ ] **Step 3: 启动后端**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw spring-boot:run
```

预期：Spring Boot 在 8080 端口启动。

- [ ] **Step 4: 用 curl 冒烟测试登录和发布**

另开一个终端执行:

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/user/wx-login \
  -H 'Content-Type: application/json' \
  -d '{"openid":"sprint2_smoke_user"}' | sed -n 's/.*"data":"\([^"]*\)".*/\1/p')

POST_RESPONSE=$(curl -s -X POST http://127.0.0.1:8080/api/posts \
  -H "Content-Type: application/json" \
  -H "satoken: $TOKEN" \
  -d '{
    "postType": "LOST",
    "title": "丢失校园卡",
    "itemName": "校园卡",
    "itemCategory": "证件",
    "description": "绿色卡套",
    "privateFeature": "卡号后四位 1234",
    "campusArea": "南校园",
    "locationName": "逸夫楼",
    "longitude": 113.2931234,
    "latitude": 23.0961234,
    "storageLocation": "",
    "eventTime": "2026-05-13T09:30:00"
  }')

echo "$POST_RESPONSE"
POST_ID=$(echo "$POST_RESPONSE" | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p')

curl -i -X GET "http://127.0.0.1:8080/api/posts/$POST_ID" \
  -H "satoken: $TOKEN"
```

发布接口预期响应包含:

```json
{"code":200,"message":"success","data":{"id":1,"postType":"LOST","title":"丢失校园卡","itemName":"校园卡","itemCategory":"证件","campusArea":"南校园","locationName":"逸夫楼","eventTime":"2026-05-13T09:30:00","status":"PENDING_AUDIT"}}
```

`id` 可以是任意正数。

详情接口预期响应包含同一个 `id`、`title` 和 `status=PENDING_AUDIT`。

- [ ] **Step 5: 冒烟测试未登录发布**

```bash
curl -i -X POST http://127.0.0.1:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"postType":"LOST","title":"x","itemName":"x","itemCategory":"x","campusArea":"x","locationName":"x","eventTime":"2026-05-13T09:30:00"}'
```

预期：HTTP 401，并返回 `{"code":401,"message":"未登录或登录已过期","data":null}`.

- [ ] **Step 6: 最终 Commit**

```bash
git status --short
git add backend miniapp docs/superpowers/plans/2026-05-13-sprint-2-publish-post.md
git commit -m "feat: complete sprint 2 publish posts"
```

---

## 自查

- 规格覆盖:
  - `POST /api/posts` 和 `GET /api/posts/{id}` 在 Task 3 实现。
  - 发布表单校验和小程序提交流程在 Task 4 实现。
  - 新单据进入 `PENDING_AUDIT` 在 Task 2 实现。
  - 封禁用户不能发布在 Task 2 覆盖。
  - S3 列表筛选、S4 审核 UI、图片、匹配、通知、地图点位都明确不属于 Sprint 2。
- 占位符扫描:
  - 没有占位符标记或未说明清楚的实现步骤。
  - 每个改代码的步骤都包含具体文件内容或精确替换说明。
- 类型一致性:
  - 请求 DTO 使用 `postType`, `title`, `itemName`, `itemCategory`, `description`, `privateFeature`, `campusArea`, `locationName`, `longitude`, `latitude`, `storageLocation`, `eventTime`.
  - 后端实体和小程序 payload 使用相同字段名。
  - 状态值符合新版总设计：`PENDING_AUDIT`、`MATCHING`、`CLAIMING`、`RETURNING`、`COMPLETED`；不引入 `DELETED` 状态。

---

### 验证清单

- [ ] 后端全部测试通过 (`./mvnw test`)
- [ ] 管理端构建通过 (`npm run build`)
- [ ] `POST /api/posts` 未登录返回 401
- [ ] `POST /api/posts` 登录后创建单据并返回 `PENDING_AUDIT`
- [ ] `GET /api/posts/{id}` 登录后返回刚创建的单据详情
- [ ] 封禁用户发布时返回“用户已被封禁”
- [ ] 小程序 tabBar 显示：首页 / 发布 / 我的
- [ ] 小程序发布页可选择寻物 `LOST` 和招领 `FOUND`
- [ ] 小程序发布表单必填项为空时显示 toast
- [ ] 小程序日期字段使用原生 date picker
- [ ] 小程序未登录提交时跳转到“我的”
- [ ] 小程序登录后提交成功提示“提交成功，等待审核”
