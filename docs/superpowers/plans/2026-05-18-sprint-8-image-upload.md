# Sprint 8: 单据图片上传 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为小程序发布单据补上最多 3 张图片的上传、保存和展示闭环，并让管理端可以只读查看图片。

**Architecture:** 后端新增 `post_image` 从表和 `/api/files/upload` 上传接口，文件先落本地目录，再由 `POST /api/posts` 引用上传后的 URL。查询单据时列表只返回 `coverImageUrl`，详情返回 `imageUrls`；小程序发布页负责选图、上传、预览和删除，首页卡片与详情页消费新的图片字段，管理端审核详情页只读展示。

**Tech Stack:** Spring Boot 3.5.14, MyBatis-Plus 3.5.16, Sa-Token 1.45.0, Java 21, Maven, 原生微信小程序 `wx.chooseMedia` / `wx.uploadFile`, Vue 3 + Element Plus。

---

## 文件结构

- Modify: `scripts/init_schema.sql`  
  职责：新增 `post_image` 表。
- Create: `backend/src/main/java/com/shigui/entity/PostImage.java`  
  职责：`post_image` 表实体。
- Create: `backend/src/main/java/com/shigui/mapper/PostImageMapper.java`  
  职责：图片记录基础读写。
- Create: `backend/src/main/java/com/shigui/dto/FileUploadResponse.java`  
  职责：上传接口返回单图 URL。
- Modify: `backend/src/main/java/com/shigui/dto/CreatePostRequest.java`  
  职责：发布请求新增 `imageUrls`。
- Modify: `backend/src/main/java/com/shigui/dto/PostResponse.java`  
  职责：列表/详情响应新增 `coverImageUrl` 与 `imageUrls`。
- Create: `backend/src/main/java/com/shigui/controller/FileController.java`  
  职责：接收图片上传请求。
- Create: `backend/src/main/java/com/shigui/service/FileStorageService.java`  
  职责：文件存储接口。
- Create: `backend/src/main/java/com/shigui/service/impl/LocalFileStorageServiceImpl.java`  
  职责：本地目录存储、文件类型/大小校验、URL 生成。
- Modify: `backend/src/main/java/com/shigui/service/LostFoundPostService.java`  
  职责：发布/查询契约支持图片字段。
- Modify: `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`  
  职责：发布单据时写入 `post_image`，查询时聚合 `coverImageUrl` / `imageUrls`。
- Modify: `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`  
  职责：发布接口接收 `imageUrls`，列表/详情返回图片字段。
- Create: `backend/src/main/java/com/shigui/config/WebMvcResourceConfig.java`  
  职责：把 `/uploads/**` 暴露为静态资源。
- Modify: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`  
  职责：发布和查询图片的 Service 测试。
- Create: `backend/src/test/java/com/shigui/service/LocalFileStorageServiceTest.java`  
  职责：上传校验与本地落盘路径测试。
- Create: `backend/src/test/java/com/shigui/controller/FileControllerTest.java`  
  职责：上传接口 HTTP 契约测试。
- Modify: `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java`  
  职责：发布接口和详情/列表图片字段契约测试。
- Modify: `miniapp/pages/publish-form/publish-form.js`  
  职责：选图、删除、上传、提交 `imageUrls`。
- Modify: `miniapp/pages/publish-form/publish-form.wxml`  
  职责：图片上传区 UI。
- Modify: `miniapp/pages/publish-form/publish-form.wxss`  
  职责：图片上传区样式。
- Modify: `miniapp/components/post-card/post-card.js`  
  职责：消费 `coverImageUrl`。
- Modify: `miniapp/components/post-card/post-card.wxml`  
  职责：有图显示真实图片，无图显示占位图。
- Modify: `miniapp/components/post-card/post-card.wxss`  
  职责：首图显示样式。
- Modify: `miniapp/pages/detail/detail.js`  
  职责：详情页消费 `imageUrls` 并支持预览。
- Modify: `miniapp/pages/detail/detail.wxml`  
  职责：图片轮播或横向图集。
- Modify: `miniapp/pages/detail/detail.wxss`  
  职责：详情页图片区域样式。
- Modify: `admin-web/src/views/PostAuditView.vue`  
  职责：审核详情里只读显示图片。
- Modify: `admin-web/src/types/api.ts` 或现有 API 类型文件  
  职责：声明 `coverImageUrl` / `imageUrls`。
- Create: `miniapp/tests/image-upload.test.js`  
  职责：小程序选图、删除和提交 payload 的轻量回归测试。

---

### Task 1: 数据库与后端 DTO/Entity 契约

**Files:**
- Modify: `scripts/init_schema.sql`
- Create: `backend/src/main/java/com/shigui/entity/PostImage.java`
- Create: `backend/src/main/java/com/shigui/mapper/PostImageMapper.java`
- Create: `backend/src/main/java/com/shigui/dto/FileUploadResponse.java`
- Modify: `backend/src/main/java/com/shigui/dto/CreatePostRequest.java`
- Modify: `backend/src/main/java/com/shigui/dto/PostResponse.java`
- Test: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`

- [ ] **Step 1: 先写会失败的 DTO 契约测试**

在 `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java` 增加 import：

```java
import com.shigui.dto.FileUploadResponse;

import java.util.List;
```

在测试类里追加：

```java
@Test
void compileContract_postImageAndDtoFieldsExist() {
    CreatePostRequest request = new CreatePostRequest();
    request.setImageUrls(List.of("/uploads/posts/2026/05/18/a.jpg"));

    PostResponse response = new PostResponse();
    response.setCoverImageUrl("/uploads/posts/2026/05/18/a.jpg");
    response.setImageUrls(List.of("/uploads/posts/2026/05/18/a.jpg", "/uploads/posts/2026/05/18/b.jpg"));

    FileUploadResponse uploadResponse = new FileUploadResponse();
    uploadResponse.setUrl("/uploads/posts/2026/05/18/a.jpg");

    PostImage image = new PostImage();
    image.setPostId(10L);
    image.setImageUrl("/uploads/posts/2026/05/18/a.jpg");
    image.setSortOrder(0);

    assertThat(request.getImageUrls()).hasSize(1);
    assertThat(response.getCoverImageUrl()).contains("/uploads/posts/");
    assertThat(response.getImageUrls()).hasSize(2);
    assertThat(uploadResponse.getUrl()).contains("/uploads/posts/");
    assertThat(PostImageMapper.class).isInterface();
}
```

- [ ] **Step 2: 运行测试，确认红灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest
```

预期：FAIL，报 `PostImage`、`PostImageMapper`、`FileUploadResponse`、`setImageUrls`、`setCoverImageUrl` 等 symbol not found。

- [ ] **Step 3: 更新数据库脚本**

在 `scripts/init_schema.sql` 的 `lost_found_post` 表后追加：

```sql
CREATE TABLE post_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort_order INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_post_sort (post_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: 创建图片实体与 Mapper**

创建 `backend/src/main/java/com/shigui/entity/PostImage.java`：

```java
package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_image")
public class PostImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private String imageUrl;
    private Integer sortOrder;
    private Integer deleted;
    private LocalDateTime createdAt;
}
```

创建 `backend/src/main/java/com/shigui/mapper/PostImageMapper.java`：

```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.PostImage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostImageMapper extends BaseMapper<PostImage> {
}
```

- [ ] **Step 5: 扩展 DTO**

创建 `backend/src/main/java/com/shigui/dto/FileUploadResponse.java`：

```java
package com.shigui.dto;

import lombok.Data;

@Data
public class FileUploadResponse {
    private String url;
}
```

修改 `backend/src/main/java/com/shigui/dto/CreatePostRequest.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> imageUrls;
}
```

修改 `backend/src/main/java/com/shigui/dto/PostResponse.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private LocalDateTime publishedAt;
    private String status;
    private String coverImageUrl;
    private List<String> imageUrls;
}
```

- [ ] **Step 6: 运行测试，确认绿灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest
```

预期：PASS，`LostFoundPostServiceTest` 通过。

- [ ] **Step 7: Commit**

```bash
git add scripts/init_schema.sql \
        backend/src/main/java/com/shigui/entity/PostImage.java \
        backend/src/main/java/com/shigui/mapper/PostImageMapper.java \
        backend/src/main/java/com/shigui/dto/FileUploadResponse.java \
        backend/src/main/java/com/shigui/dto/CreatePostRequest.java \
        backend/src/main/java/com/shigui/dto/PostResponse.java \
        backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java
git commit -m "feat: add post image schema and DTOs"
```

---

### Task 2: 本地文件上传接口与静态资源映射

**Files:**
- Create: `backend/src/main/java/com/shigui/service/FileStorageService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/LocalFileStorageServiceImpl.java`
- Create: `backend/src/main/java/com/shigui/controller/FileController.java`
- Create: `backend/src/main/java/com/shigui/config/WebMvcResourceConfig.java`
- Create: `backend/src/test/java/com/shigui/service/LocalFileStorageServiceTest.java`
- Create: `backend/src/test/java/com/shigui/controller/FileControllerTest.java`

- [ ] **Step 1: 写会失败的上传 Service 测试**

创建 `backend/src/test/java/com/shigui/service/LocalFileStorageServiceTest.java`：

```java
package com.shigui.service;

import com.shigui.service.impl.LocalFileStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeImage_validPng_returnsUploadsUrlAndWritesFile() throws Exception {
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(tempDir.toString(), "/uploads");
        MockMultipartFile file = new MockMultipartFile(
                "file", "demo.png", "image/png", new byte[]{1, 2, 3, 4}
        );

        String url = service.storeImage(file);

        assertThat(url).startsWith("/uploads/posts/");
        assertThat(tempDir.resolve(url.replace("/uploads/", "")).toFile().exists()).isTrue();
    }

    @Test
    void storeImage_nonImage_throwsException() {
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(tempDir.toString(), "/uploads");
        MockMultipartFile file = new MockMultipartFile(
                "file", "demo.txt", "text/plain", "hello".getBytes()
        );

        assertThatThrownBy(() -> service.storeImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅支持图片上传");
    }
}
```

- [ ] **Step 2: 运行测试，确认红灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LocalFileStorageServiceTest
```

预期：FAIL，提示 `FileStorageService` / `LocalFileStorageServiceImpl` 不存在。

- [ ] **Step 3: 实现文件存储 Service**

创建 `backend/src/main/java/com/shigui/service/FileStorageService.java`：

```java
package com.shigui.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeImage(MultipartFile file);
}
```

创建 `backend/src/main/java/com/shigui/service/impl/LocalFileStorageServiceImpl.java`：

```java
package com.shigui.service.impl;

import com.shigui.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final String uploadRoot;
    private final String publicPrefix;

    public LocalFileStorageServiceImpl(
            @Value("${app.upload.root:uploads}") String uploadRoot,
            @Value("${app.upload.public-prefix:/uploads}") String publicPrefix
    ) {
        this.uploadRoot = uploadRoot;
        this.publicPrefix = publicPrefix;
    }

    @Override
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片不能为空");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("仅支持图片上传");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("图片不能超过 5MB");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".jpg";
        LocalDate today = LocalDate.now();
        Path relativePath = Paths.get("posts",
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                UUID.randomUUID() + extension);
        Path fullPath = Paths.get(uploadRoot).resolve(relativePath);

        try {
            Files.createDirectories(fullPath.getParent());
            file.transferTo(fullPath);
        } catch (IOException e) {
            throw new IllegalStateException("保存图片失败", e);
        }

        return publicPrefix + "/" + relativePath.toString().replace("\\\\", "/");
    }
}
```

- [ ] **Step 4: 实现上传 Controller 和静态映射**

创建 `backend/src/main/java/com/shigui/controller/FileController.java`：

```java
package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.dto.FileUploadResponse;
import com.shigui.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public Result<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        if (!StpUtil.isLogin()) {
            return Result.fail(401, "请先登录");
        }
        FileUploadResponse response = new FileUploadResponse();
        response.setUrl(fileStorageService.storeImage(file));
        return Result.ok(response);
    }
}
```

创建 `backend/src/main/java/com/shigui/config/WebMvcResourceConfig.java`：

```java
package com.shigui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadRoot).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
```

- [ ] **Step 5: 写并实现上传接口契约测试**

创建 `backend/src/test/java/com/shigui/controller/FileControllerTest.java`：

```java
package com.shigui.controller;

import com.shigui.service.AppUserService;
import com.shigui.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private AppUserService appUserService;

    @Test
    void upload_notLoggedIn_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void upload_loggedIn_returnsUrl() throws Exception {
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        when(fileStorageService.storeImage(any())).thenReturn("/uploads/posts/2026/05/18/a.png");
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        String token = mockMvc.perform(post("/api/user/wx-login")
                        .contentType("application/json")
                        .content("{\"openid\":\"upload_test_user\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"data\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(multipart("/api/files/upload").file(file).header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value("/uploads/posts/2026/05/18/a.png"));
    }
}
```

- [ ] **Step 6: 跑测试确认绿灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LocalFileStorageServiceTest,FileControllerTest
```

预期：PASS。

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/shigui/service/FileStorageService.java \
        backend/src/main/java/com/shigui/service/impl/LocalFileStorageServiceImpl.java \
        backend/src/main/java/com/shigui/controller/FileController.java \
        backend/src/main/java/com/shigui/config/WebMvcResourceConfig.java \
        backend/src/test/java/com/shigui/service/LocalFileStorageServiceTest.java \
        backend/src/test/java/com/shigui/controller/FileControllerTest.java
git commit -m "feat: add local image upload API"
```

---

### Task 3: 发布与查询接口接入图片数据

**Files:**
- Modify: `backend/src/main/java/com/shigui/service/LostFoundPostService.java`
- Modify: `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`
- Modify: `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`
- Modify: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`
- Modify: `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java`

- [ ] **Step 1: 先写会失败的 Service 行为测试**

在 `LostFoundPostServiceTest` 追加：

```java
@Test
void publish_withThreeImageUrls_savesPostImages() {
    AppUser user = normalUser(2L);
    CreatePostRequest request = validLostRequest();
    request.setImageUrls(List.of(
            "/uploads/posts/2026/05/18/a.jpg",
            "/uploads/posts/2026/05/18/b.jpg",
            "/uploads/posts/2026/05/18/c.jpg"
    ));
    when(appUserService.getByIdOrThrow(2L)).thenReturn(user);
    when(lostFoundPostMapper.insert(any(LostFoundPost.class))).thenAnswer(inv -> {
        LostFoundPost post = inv.getArgument(0);
        post.setId(22L);
        return 1;
    });

    PostResponse response = lostFoundPostService.publish(2L, request);

    assertThat(response.getImageUrls()).hasSize(3);
    assertThat(response.getCoverImageUrl()).isEqualTo("/uploads/posts/2026/05/18/a.jpg");
}

@Test
void publish_moreThanThreeImages_throwsException() {
    CreatePostRequest request = validLostRequest();
    request.setImageUrls(List.of("a", "b", "c", "d"));
    when(appUserService.getByIdOrThrow(1L)).thenReturn(normalUser(1L));

    assertThatThrownBy(() -> lostFoundPostService.publish(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("最多上传 3 张图片");
}
```

- [ ] **Step 2: 写会失败的 Controller 契约测试**

在 `LostFoundPostControllerTest` 的 `validJson()` 返回值里加入：

```json
"imageUrls": [
  "/uploads/posts/2026/05/18/a.jpg",
  "/uploads/posts/2026/05/18/b.jpg"
]
```

并在 `publish_loggedIn_returnsPostResponse` 断言中增加：

```java
.andExpect(jsonPath("$.data.coverImageUrl").value("/uploads/posts/2026/05/18/a.jpg"))
.andExpect(jsonPath("$.data.imageUrls[0]").value("/uploads/posts/2026/05/18/a.jpg"))
```

- [ ] **Step 3: 运行测试，确认红灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest,LostFoundPostControllerTest
```

预期：FAIL，图片字段未映射、图片数量未校验。

- [ ] **Step 4: 在 Service 中写入/读取图片**

修改 `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`：

- 构造函数注入 `PostImageMapper`
- `validate()` 追加：

```java
List<String> imageUrls = request.getImageUrls();
if (imageUrls != null && imageUrls.size() > 3) {
    throw new IllegalArgumentException("最多上传 3 张图片");
}
if (imageUrls != null) {
    for (String imageUrl : imageUrls) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/posts/")) {
            throw new IllegalArgumentException("图片地址非法");
        }
    }
}
```

- `publish()` 保存 `lost_found_post` 后追加：

```java
savePostImages(post.getId(), request.getImageUrls());
PostResponse response = toResponse(post);
response.setImageUrls(request.getImageUrls() == null ? List.of() : request.getImageUrls());
response.setCoverImageUrl(response.getImageUrls().isEmpty() ? null : response.getImageUrls().get(0));
return response;
```

- `toResponse()` 改成聚合查询图片：

```java
List<String> imageUrls = listPostImageUrls(post.getId());
response.setImageUrls(imageUrls);
response.setCoverImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
```

- 增加 helper：

```java
private void savePostImages(Long postId, List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
        return;
    }
    for (int i = 0; i < imageUrls.size(); i++) {
        PostImage image = new PostImage();
        image.setPostId(postId);
        image.setImageUrl(imageUrls.get(i));
        image.setSortOrder(i);
        image.setDeleted(0);
        postImageMapper.insert(image);
    }
}

private List<String> listPostImageUrls(Long postId) {
    LambdaQueryWrapper<PostImage> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(PostImage::getPostId, postId);
    wrapper.eq(PostImage::getDeleted, 0);
    wrapper.orderByAsc(PostImage::getSortOrder);
    return postImageMapper.selectList(wrapper).stream().map(PostImage::getImageUrl).toList();
}
```

- [ ] **Step 5: 跑测试确认绿灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest,LostFoundPostControllerTest
```

预期：PASS。

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/shigui/service/LostFoundPostService.java \
        backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java \
        backend/src/main/java/com/shigui/controller/LostFoundPostController.java \
        backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java \
        backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java
git commit -m "feat: attach images to posts"
```

---

### Task 4: 小程序发布、首页卡片和详情图集

**Files:**
- Modify: `miniapp/pages/publish-form/publish-form.js`
- Modify: `miniapp/pages/publish-form/publish-form.wxml`
- Modify: `miniapp/pages/publish-form/publish-form.wxss`
- Modify: `miniapp/components/post-card/post-card.js`
- Modify: `miniapp/components/post-card/post-card.wxml`
- Modify: `miniapp/components/post-card/post-card.wxss`
- Modify: `miniapp/pages/detail/detail.js`
- Modify: `miniapp/pages/detail/detail.wxml`
- Modify: `miniapp/pages/detail/detail.wxss`
- Create: `miniapp/tests/image-upload.test.js`

- [ ] **Step 1: 写会失败的小程序回归测试**

创建 `miniapp/tests/image-upload.test.js`：

```javascript
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function loadPage(wxOverrides = {}) {
  const filePath = path.join(__dirname, '..', 'pages', 'publish-form', 'publish-form.js');
  const source = fs.readFileSync(filePath, 'utf8');
  let pageConfig;
  const context = vm.createContext({
    console,
    getApp: () => ({ globalData: { baseUrl: 'http://localhost:8080', token: 'demo-token' } }),
    Page: (config) => { pageConfig = config; },
    wx: { chooseMedia() {}, uploadFile() {}, showToast() {}, request() {}, ...wxOverrides }
  });
  vm.runInContext(source, context, { filename: filePath });
  return pageConfig;
}

function createInstance(wxOverrides = {}) {
  const config = loadPage(wxOverrides);
  const instance = {
    data: structuredClone(config.data),
    setData(updates) {
      Object.entries(updates).forEach(([key, value]) => {
        const parts = key.split('.');
        let target = this.data;
        while (parts.length > 1) {
          target = target[parts.shift()];
        }
        target[parts[0]] = value;
      });
    }
  };
  Object.entries(config).forEach(([key, value]) => {
    if (typeof value === 'function') instance[key] = value.bind(instance);
  });
  return instance;
}

test('addImages limits count to three', () => {
  const page = createInstance({
    chooseMedia(options) {
      options.success({
        tempFiles: [{ tempFilePath: 'a.jpg' }, { tempFilePath: 'b.jpg' }, { tempFilePath: 'c.jpg' }, { tempFilePath: 'd.jpg' }]
      });
    }
  });

  page.addImages();

  assert.equal(page.data.localImages.length, 3);
});
```

- [ ] **Step 2: 运行测试，确认红灯**

```bash
cd /Users/cyrene/Dev/shigui
node --test miniapp/tests/image-upload.test.js
```

预期：FAIL，`addImages` / `localImages` 不存在。

- [ ] **Step 3: 实现发布页图片选择与上传**

在 `miniapp/pages/publish-form/publish-form.js` 中：

- `data` 增加：

```javascript
localImages: [],
uploadedImageUrls: [],
uploadingImages: false
```

- 增加方法：

```javascript
addImages() {
  const remain = 3 - this.data.localImages.length
  if (remain <= 0) {
    wx.showToast({ title: '最多上传 3 张图片', icon: 'none' })
    return
  }
  wx.chooseMedia({
    count: remain,
    mediaType: ['image'],
    success: (res) => {
      const nextImages = [...this.data.localImages, ...res.tempFiles.map(file => file.tempFilePath)].slice(0, 3)
      this.setData({ localImages: nextImages })
    }
  })
},

removeImage(e) {
  const index = Number(e.currentTarget.dataset.index)
  const nextImages = this.data.localImages.filter((_, idx) => idx !== index)
  const nextUrls = this.data.uploadedImageUrls.filter((_, idx) => idx !== index)
  this.setData({ localImages: nextImages, uploadedImageUrls: nextUrls })
},

previewImage(e) {
  const current = e.currentTarget.dataset.url
  wx.previewImage({ current, urls: this.data.localImages })
},

uploadImages() {
  if (this.data.localImages.length === 0) {
    return Promise.resolve([])
  }
  this.setData({ uploadingImages: true })
  const token = app.globalData.token
  const uploads = this.data.localImages.map((filePath) => new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${app.globalData.baseUrl}/api/files/upload`,
      filePath,
      name: 'file',
      header: { satoken: token },
      success: (res) => {
        const body = JSON.parse(res.data)
        if (body.code === 200 && body.data && body.data.url) {
          resolve(body.data.url)
          return
        }
        reject(new Error(body.message || '上传失败'))
      },
      fail: () => reject(new Error('网络错误'))
    })
  }))
  return Promise.all(uploads).finally(() => this.setData({ uploadingImages: false }))
}
```

- `submit()` 中，在构造 payload 前插入：

```javascript
this.uploadImages()
  .then((imageUrls) => {
    const payload = {
      // 原有字段
      imageUrls
    }
    // 保持原来的 wx.request 提交逻辑
  })
  .catch((error) => {
    wx.showToast({ title: error.message || '图片上传失败', icon: 'none' })
    this.setData({ submitting: false })
  })
```

- [ ] **Step 4: 实现发布页/卡片/详情 UI**

在 `miniapp/pages/publish-form/publish-form.wxml` 的描述区前增加：

```xml
<view class="form-group">
  <view class="form-label">物品图片</view>
  <view class="image-grid">
    <view class="image-item" wx:for="{{localImages}}" wx:key="*this">
      <image class="image-thumb" src="{{item}}" mode="aspectFill" data-url="{{item}}" bindtap="previewImage" />
      <button class="image-remove" size="mini" data-index="{{index}}" bindtap="removeImage">删除</button>
    </view>
    <button wx:if="{{localImages.length < 3}}" class="image-add" bindtap="addImages">添加图片</button>
  </view>
</view>
```

在 `miniapp/components/post-card/post-card.wxml` 中把占位图替换为：

```xml
<view class="image-placeholder">
  <image wx:if="{{item.coverImageUrl}}" class="cover-image" src="{{item.coverImageUrl}}" mode="aspectFill" />
  <image wx:else class="image-icon" src="/assets/icons/image.svg" />
</view>
```

在 `miniapp/pages/detail/detail.wxml` 的 hero 区后插入：

```xml
<scroll-view wx:if="{{post.imageUrls && post.imageUrls.length}}" class="image-strip" scroll-x="true">
  <image wx:for="{{post.imageUrls}}" wx:key="*this" class="detail-image" src="{{item}}" mode="aspectFill" data-url="{{item}}" bindtap="previewDetailImage" />
</scroll-view>
```

并在 `detail.js` 增加：

```javascript
previewDetailImage(e) {
  const current = e.currentTarget.dataset.url
  wx.previewImage({ current, urls: this.data.post.imageUrls || [] })
}
```

- [ ] **Step 5: 跑测试和静态检查**

```bash
cd /Users/cyrene/Dev/shigui
node --test miniapp/tests/image-upload.test.js
node --check miniapp/pages/publish-form/publish-form.js
node --check miniapp/pages/detail/detail.js
node --check miniapp/components/post-card/post-card.js
```

预期：PASS。

- [ ] **Step 6: Commit**

```bash
git add miniapp/pages/publish-form/publish-form.js \
        miniapp/pages/publish-form/publish-form.wxml \
        miniapp/pages/publish-form/publish-form.wxss \
        miniapp/components/post-card/post-card.js \
        miniapp/components/post-card/post-card.wxml \
        miniapp/components/post-card/post-card.wxss \
        miniapp/pages/detail/detail.js \
        miniapp/pages/detail/detail.wxml \
        miniapp/pages/detail/detail.wxss \
        miniapp/tests/image-upload.test.js
git commit -m "feat: add miniapp post image upload flow"
```

---

### Task 5: 管理端只读展示与全量验证

**Files:**
- Modify: `admin-web/src/views/PostAuditView.vue`
- Modify: `admin-web/src/types/api.ts`

- [ ] **Step 1: 写会失败的类型约束**

如果 `admin-web/src/types/api.ts` 已存在 `Post` 类型，改为：

```ts
export interface PostResponse {
  id: number
  postType: string
  title: string
  itemName: string
  itemCategory: string
  description: string
  campusArea: string
  locationName: string
  storageLocation: string
  eventTime: string
  publishedAt: string
  status: string
  coverImageUrl?: string | null
  imageUrls: string[]
}
```

- [ ] **Step 2: 在审核详情中显示图片**

在 `admin-web/src/views/PostAuditView.vue` 的详情抽屉中，在描述区前加入：

```vue
<el-form-item label="物品图片" v-if="currentPost?.imageUrls?.length">
  <div class="post-image-list">
    <el-image
      v-for="url in currentPost.imageUrls"
      :key="url"
      :src="url"
      fit="cover"
      preview-teleported
      :preview-src-list="currentPost.imageUrls"
      class="post-image"
    />
  </div>
</el-form-item>
```

并在同文件 `<style scoped>` 里加入：

```css
.post-image-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.post-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
}
```

- [ ] **Step 3: 跑管理端构建**

```bash
cd /Users/cyrene/Dev/shigui/admin-web
npm run build
```

预期：PASS。

- [ ] **Step 4: 跑后端全量测试**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test
```

预期：PASS，已有测试与新增图片相关测试全部通过。

- [ ] **Step 5: 做人工验收清单**

在微信开发者工具中验证：

- 发布页可选 1-3 张图
- 第 4 张被拦截
- 删除图片后可重新补选
- 发布成功后首页卡片显示首图
- 详情页显示全部图片

在管理端验证：

- 审核详情可看到图片
- 无图旧单据不报错

- [ ] **Step 6: Commit**

```bash
git add admin-web/src/views/PostAuditView.vue \
        admin-web/src/types/api.ts
git commit -m "feat: show post images in admin review"
```

---

## 验证清单

- [ ] `scripts/init_schema.sql` 已新增 `post_image` 表
- [ ] `POST /api/files/upload` 可上传单张图片并返回 URL
- [ ] `POST /api/posts` 可接收 `imageUrls`
- [ ] 发布单据最多 3 张图，超出会被拦截
- [ ] 首页列表显示 `coverImageUrl`
- [ ] 详情页显示 `imageUrls`
- [ ] 管理端审核详情可只读查看图片
- [ ] `backend ./mvnw test` 通过
- [ ] `admin-web npm run build` 通过
- [ ] `node --test miniapp/tests/image-upload.test.js` 通过
