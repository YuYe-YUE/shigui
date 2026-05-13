# Sprint 4: 内容审核 + 用户管理 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现管理端内容审核（查看待审核/全量单据 + 审核通过 + 删除违规）和用户管理（查看用户列表 + 封禁/解封），全部对接后端 API。

**Architecture:** 后端扩展现有 AdminController，新增审核和用户管理端点。新建 AuditRecord 实体记录操作日志。管理员登录后操作受 SaToken 保护。管理端新增两个完整页面：PostAuditView 和 UserManageView。

**Tech Stack:** Spring Boot 3.5.14, MyBatis-Plus 3.5.16, Sa-Token 1.45.0, Java 21, Vue 3 + Element Plus + TypeScript。

---

## 文件结构

- Create: `backend/src/main/java/com/shigui/entity/AuditRecord.java`
- Create: `backend/src/main/java/com/shigui/mapper/AuditRecordMapper.java`
- Create: `backend/src/main/java/com/shigui/service/AuditRecordService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/AuditRecordServiceImpl.java`
- Modify: `backend/src/main/java/com/shigui/controller/AdminController.java`
- Modify: `backend/src/main/java/com/shigui/service/AppUserService.java`
- Modify: `backend/src/main/java/com/shigui/service/impl/AppUserServiceImpl.java`
- Create: `backend/src/test/java/com/shigui/service/AdminPostServiceTest.java`
- Modify: `backend/src/test/java/com/shigui/controller/AdminControllerTest.java`
- Create: `admin-web/src/views/PostAuditView.vue`
- Create: `admin-web/src/views/UserManageView.vue`
- Modify: `admin-web/src/router/index.ts`
- Modify: `admin-web/src/layouts/MainLayout.vue`

---

### Task 1: AuditRecord 实体 + Mapper + Service

**Files:**
- Create: `backend/src/main/java/com/shigui/entity/AuditRecord.java`
- Create: `backend/src/main/java/com/shigui/mapper/AuditRecordMapper.java`
- Create: `backend/src/main/java/com/shigui/service/AuditRecordService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/AuditRecordServiceImpl.java`

- [ ] **Step 1: AuditRecord 实体**

```java
package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audit_record")
public class AuditRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private Long postId;
    private String action;
    private String reason;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Mapper**

```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {
}
```

- [ ] **Step 3: Service 接口**

```java
package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.entity.AuditRecord;

public interface AuditRecordService extends IService<AuditRecord> {
    void logApprove(Long adminId, Long postId);
    void logDelete(Long adminId, Long postId, String reason);
}
```

- [ ] **Step 4: Service 实现**

```java
package com.shigui.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.entity.AuditRecord;
import com.shigui.mapper.AuditRecordMapper;
import com.shigui.service.AuditRecordService;
import org.springframework.stereotype.Service;

@Service
public class AuditRecordServiceImpl extends ServiceImpl<AuditRecordMapper, AuditRecord> implements AuditRecordService {

    @Override
    public void logApprove(Long adminId, Long postId) {
        AuditRecord record = new AuditRecord();
        record.setAdminId(adminId);
        record.setPostId(postId);
        record.setAction("APPROVE");
        save(record);
    }

    @Override
    public void logDelete(Long adminId, Long postId, String reason) {
        AuditRecord record = new AuditRecord();
        record.setAdminId(adminId);
        record.setPostId(postId);
        record.setAction("DELETE");
        record.setReason(reason != null ? reason : "");
        save(record);
    }
}
```

- [ ] **Step 5: 验证编译**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw compile
```
预期：BUILD SUCCESS。

- [ ] **Step 6: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add backend/src/main/java/com/shigui/entity/AuditRecord.java backend/src/main/java/com/shigui/mapper/AuditRecordMapper.java backend/src/main/java/com/shigui/service/AuditRecordService.java backend/src/main/java/com/shigui/service/impl/AuditRecordServiceImpl.java
git -C /Users/cyrene/Dev/shigui commit -m "feat: add AuditRecord entity, mapper, service"
```

---

### Task 2: 管理端审核 API（TDD）

**Files:**
- Create: `backend/src/test/java/com/shigui/service/AdminPostServiceTest.java`
- Modify: `backend/src/main/java/com/shigui/controller/AdminController.java`
- Modify: `backend/src/test/java/com/shigui/controller/AdminControllerTest.java`

- [ ] **Step 1: 编写 Service 测试**

创建 `backend/src/test/java/com/shigui/service/AdminPostServiceTest.java`：

```java
package com.shigui.service;

import com.shigui.entity.AuditRecord;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.AuditRecordMapper;
import com.shigui.mapper.LostFoundPostMapper;
import com.shigui.service.impl.AuditRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminPostServiceTest {

    private AuditRecordMapper auditRecordMapper;
    private LostFoundPostMapper lostFoundPostMapper;
    private AuditRecordService auditRecordService;

    @BeforeEach
    void setUp() {
        auditRecordMapper = mock(AuditRecordMapper.class);
        lostFoundPostMapper = mock(LostFoundPostMapper.class);
        auditRecordService = new AuditRecordServiceImpl();
        injectMapper(auditRecordService, auditRecordMapper);
    }

    @Test
    void logApprove_createsApproveRecord() {
        auditRecordService.logApprove(1L, 100L);
        verify(auditRecordMapper).insert(any(AuditRecord.class));
    }

    @Test
    void logDelete_createsDeleteRecordWithReason() {
        auditRecordService.logDelete(1L, 100L, "违规内容");
        verify(auditRecordMapper).insert(any(AuditRecord.class));
    }

    private void injectMapper(Object service, Object mapper) {
        try {
            var field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=AdminPostServiceTest
```
预期：PASS — 2 tests。

- [ ] **Step 3: 编写 Controller 测试**

在 `AdminControllerTest` 中追加以下测试方法。需要新增 mock `LostFoundPostService` 和 `AuditRecordService`：

新增字段：
```java
@MockitoBean
private LostFoundPostService lostFoundPostService;

@MockitoBean
private AuditRecordService auditRecordService;
```

新增测试方法：
```java
@Test
void listPosts_notLoggedIn_returns401() throws Exception {
    mockMvc.perform(get("/api/admin/posts"))
            .andExpect(status().isUnauthorized());
}

@Test
void listPosts_loggedIn_returns200() throws Exception {
    when(adminUserService.login(anyString(), anyString())).thenReturn("token");
    String token = getAdminToken();
    when(lostFoundPostService.page(any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

    mockMvc.perform(get("/api/admin/posts")
                    .header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void approvePost_loggedIn_returns200() throws Exception {
    when(adminUserService.login(anyString(), anyString())).thenReturn("token");
    String token = getAdminToken();
    LostFoundPost post = new LostFoundPost();
    post.setId(1L);
    post.setStatus("PENDING_AUDIT");
    when(lostFoundPostService.getById(1L)).thenReturn(post);

    mockMvc.perform(post("/api/admin/posts/1/approve")
                    .header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void deletePost_loggedIn_returns200() throws Exception {
    when(adminUserService.login(anyString(), anyString())).thenReturn("token");
    String token = getAdminToken();
    LostFoundPost post = new LostFoundPost();
    post.setId(1L);
    when(lostFoundPostService.getById(1L)).thenReturn(post);

    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/posts/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("satoken", token)
                    .content("{\"reason\":\"违规\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

private String getAdminToken() throws Exception {
    String body = mockMvc.perform(post("/api/admin/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    return body.replaceAll(".*\\\"data\\\":\\\"([^\\\"]+)\\\".*", "$1");
}
```

需要新增 import：
```java
import com.shigui.entity.LostFoundPost;
import com.shigui.service.LostFoundPostService;
import com.shigui.service.AuditRecordService;
```

- [ ] **Step 4: 运行 Controller 测试确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=AdminControllerTest
```
预期：部分 FAIL（新增的审核端点还不存在）。

- [ ] **Step 5: 实现 Controller 审核端点**

修改 `AdminController.java`。注入新依赖并添加方法：

构造函数改为：
```java
private final AdminUserService adminUserService;
private final LostFoundPostService lostFoundPostService;
private final AuditRecordService auditRecordService;

public AdminController(AdminUserService adminUserService,
                       LostFoundPostService lostFoundPostService,
                       AuditRecordService auditRecordService) {
    this.adminUserService = adminUserService;
    this.lostFoundPostService = lostFoundPostService;
    this.auditRecordService = auditRecordService;
}
```

新增 import：
```java
import cn.dev33.satoken.stp.StpUtil;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.LostFoundPostService;
import com.shigui.service.AuditRecordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
```

新增方法：
```java
/**
 * 管理员查看全量单据，支持按状态筛选。
 */
@GetMapping("/posts")
public Result<Page<LostFoundPost>> listPosts(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status) {
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LostFoundPost> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
    wrapper.eq(status != null && !status.isEmpty(), LostFoundPost::getStatus, status);
    wrapper.orderByDesc(LostFoundPost::getPublishedAt);
    return Result.ok(lostFoundPostService.page(new Page<>(page, size), wrapper));
}

/**
 * 审核通过：将单据状态从 PENDING_AUDIT 改为 MATCHING。
 */
@PostMapping("/posts/{id}/approve")
public Result<Void> approvePost(@PathVariable Long id) {
    Long adminId = StpUtil.getLoginIdAsLong();
    LostFoundPost post = lostFoundPostService.getById(id);
    if (post == null) {
        return Result.fail(404, "单据不存在");
    }
    if (!"PENDING_AUDIT".equals(post.getStatus())) {
        return Result.fail(400, "只能审核待审核状态的单据");
    }
    post.setStatus("MATCHING");
    lostFoundPostService.updateById(post);
    auditRecordService.logApprove(adminId, id);
    return Result.ok();
}

/**
 * 删除单据：设置 deleted=1 并记录删除原因。
 */
@DeleteMapping("/posts/{id}")
public Result<Void> deletePost(@PathVariable Long id, @RequestBody Map<String, String> body) {
    Long adminId = StpUtil.getLoginIdAsLong();
    LostFoundPost post = lostFoundPostService.getById(id);
    if (post == null) {
        return Result.fail(404, "单据不存在");
    }
    String reason = body.getOrDefault("reason", "");
    post.setDeleted(1);
    lostFoundPostService.updateById(post);
    auditRecordService.logDelete(adminId, id, reason);
    return Result.ok();
}
```

- [ ] **Step 6: 运行控制器测试确认通过**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=AdminControllerTest
```
预期：PASS — 6 tests（原 2 + 新 4）。

- [ ] **Step 7: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add backend/src/main/java/com/shigui/controller/AdminController.java backend/src/test/java/com/shigui/controller/AdminControllerTest.java backend/src/test/java/com/shigui/service/AdminPostServiceTest.java
git -C /Users/cyrene/Dev/shigui commit -m "feat: admin post audit APIs (list, approve, delete)"
```

---

### Task 3: 用户管理 API（封禁/解封）

**Files:**
- Modify: `backend/src/main/java/com/shigui/service/AppUserService.java`
- Modify: `backend/src/main/java/com/shigui/service/impl/AppUserServiceImpl.java`
- Modify: `backend/src/main/java/com/shigui/controller/AdminController.java`
- Modify: `backend/src/test/java/com/shigui/controller/AdminControllerTest.java`

- [ ] **Step 1: AppUserService 新增方法签名**

在 `AppUserService` 接口追加：
```java
void banUser(Long userId);
void unbanUser(Long userId);
```

- [ ] **Step 2: AppUserServiceImpl 实现**

追加：
```java
@Override
public void banUser(Long userId) {
    AppUser user = getByIdOrThrow(userId);
    user.setStatus("BANNED");
    updateById(user);
}

@Override
public void unbanUser(Long userId) {
    AppUser user = getByIdOrThrow(userId);
    user.setStatus("NORMAL");
    updateById(user);
}
```

- [ ] **Step 3: AdminController 新增用户管理端点**

在 `AdminController` 中追加：

```java
import com.shigui.entity.AppUser;
import com.shigui.service.AppUserService;
```

修改构造函数注入 `AppUserService`，并追加方法：

```java
/**
 * 管理员查看小程序用户列表。
 */
@GetMapping("/users")
public Result<Page<AppUser>> listUsers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status) {
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppUser> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
    wrapper.eq(status != null && !status.isEmpty(), AppUser::getStatus, status);
    return Result.ok(appUserService.page(new Page<>(page, size), wrapper));
}

@PutMapping("/users/{id}/ban")
public Result<Void> banUser(@PathVariable Long id) {
    appUserService.banUser(id);
    return Result.ok();
}

@PutMapping("/users/{id}/unban")
public Result<Void> unbanUser(@PathVariable Long id) {
    appUserService.unbanUser(id);
    return Result.ok();
}
```

- [ ] **Step 4: AdminControllerTest 新增用户管理测试**

追加到 `AdminControllerTest`：

```java
@Test
void listUsers_loggedIn_returns200() throws Exception {
    when(adminUserService.login(anyString(), anyString())).thenReturn("token");
    String token = getAdminToken();
    when(appUserService.page(any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

    mockMvc.perform(get("/api/admin/users")
                    .header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void banUser_loggedIn_returns200() throws Exception {
    when(adminUserService.login(anyString(), anyString())).thenReturn("token");
    String token = getAdminToken();

    mockMvc.perform(put("/api/admin/users/1/ban")
                    .header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void unbanUser_loggedIn_returns200() throws Exception {
    when(adminUserService.login(anyString(), anyString())).thenReturn("token");
    String token = getAdminToken();

    mockMvc.perform(put("/api/admin/users/1/unban")
                    .header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}
```

需要新增 import 和 mockBean：
```java
@MockitoBean
private AppUserService appUserService;
```
```java
import com.shigui.service.AppUserService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
```

- [ ] **Step 5: 运行全部后端测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test
```
预期：41 tests PASS（34 + 2 AdminPostService + 5 新 Controller tests）。

- [ ] **Step 6: 更新 SaToken 配置确保 /api/admin/** 受保护**

确认 `SaTokenConfig.java` 中 `/api/admin/login` 在排除列表，其他 `/api/admin/**` 都拦截。当前配置已正确。

- [ ] **Step 7: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add backend/
git -C /Users/cyrene/Dev/shigui commit -m "feat: admin user management APIs (list, ban, unban)"
```

---

### Task 4: 管理端内容审核页面

**Files:**
- Create: `admin-web/src/views/PostAuditView.vue`
- Modify: `admin-web/src/router/index.ts`
- Modify: `admin-web/src/layouts/MainLayout.vue`

- [ ] **Step 1: PostAuditView.vue**

创建 `admin-web/src/views/PostAuditView.vue`：

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const posts = ref<any[]>([])
const activeTab = ref('PENDING_AUDIT')
const page = ref(1)
const total = ref(0)

onMounted(() => loadPosts())

function switchTab(tab: string) {
  activeTab.value = tab
  page.value = 1
  loadPosts()
}

async function loadPosts() {
  const status = activeTab.value === 'all' ? undefined : activeTab.value
  const res = await api.get('/api/admin/posts', { params: { page: page.value, size: 10, status } })
  posts.value = res.data.data.records || []
  total.value = res.data.data.total || 0
}

async function approve(id: number) {
  await api.post(`/api/admin/posts/${id}/approve`)
  ElMessage.success('审核通过')
  loadPosts()
}

async function deletePost(id: number) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请填写删除原因', '删除单据', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await api.delete(`/api/admin/posts/${id}`, { data: { reason } })
    ElMessage.success('已删除')
    loadPosts()
  } catch { /* 用户取消 */ }
}
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">内容审核</h2>
    <el-tabs v-model="activeTab" @tab-click="(t: any) => switchTab(t.paneName as string)">
      <el-tab-pane label="待审核" name="PENDING_AUDIT" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <el-table :data="posts" stripe>
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="itemCategory" label="品类" width="80" />
      <el-table-column prop="postType" label="类型" width="70">
        <template #default="{ row }">{{ row.postType === 'LOST' ? '寻物' : '招领' }}</template>
      </el-table-column>
      <el-table-column prop="locationName" label="地点" width="120" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'PENDING_AUDIT'" type="warning" size="small">待审核</el-tag>
          <el-tag v-else-if="row.status === 'MATCHING'" type="success" size="small">匹配中</el-tag>
          <el-tag v-else size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING_AUDIT'" size="small" type="success" @click="approve(row.id)">通过</el-button>
          <el-button size="small" type="danger" @click="deletePost(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" :page-size="10" @current-change="loadPosts" background layout="prev, pager, next" />
  </div>
</template>
```

- [ ] **Step 2: 更新路由**

在 `router/index.ts` 的 children 数组追加：
```typescript
{ path: 'posts', name: 'posts', component: () => import('../views/PostAuditView.vue') },
```

- [ ] **Step 3: 更新 MainLayout 菜单**

在 `MainLayout.vue` 的 `menuItems` 数组追加：
```typescript
{ path: '/posts', title: '内容审核' },
```

- [ ] **Step 4: 验证编译**

```bash
cd /Users/cyrene/Dev/shigui/admin-web && npm run build
```
预期：构建成功。

- [ ] **Step 5: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add admin-web/
git -C /Users/cyrene/Dev/shigui commit -m "feat: admin-web content audit page"
```

---

### Task 5: 管理端用户管理页面

**Files:**
- Create: `admin-web/src/views/UserManageView.vue`
- Modify: `admin-web/src/router/index.ts`
- Modify: `admin-web/src/layouts/MainLayout.vue`

- [ ] **Step 1: UserManageView.vue**

创建 `admin-web/src/views/UserManageView.vue`：

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const users = ref<any[]>([])
const page = ref(1)
const total = ref(0)

onMounted(() => loadUsers())

async function loadUsers() {
  const res = await api.get('/api/admin/users', { params: { page: page.value, size: 10 } })
  users.value = res.data.data.records || []
  total.value = res.data.data.total || 0
}

async function banUser(id: number) {
  await api.put(`/api/admin/users/${id}/ban`)
  ElMessage.success('已封禁')
  loadUsers()
}

async function unbanUser(id: number) {
  await api.put(`/api/admin/users/${id}/unban`)
  ElMessage.success('已解封')
  loadUsers()
}
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">用户管理</h2>

    <el-table :data="users" stripe>
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'NORMAL'" type="success" size="small">正常</el-tag>
          <el-tag v-else type="danger" size="small">封禁</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button v-if="row.status === 'NORMAL'" size="small" type="danger" @click="banUser(row.id)">封禁</el-button>
          <el-button v-else size="small" type="success" @click="unbanUser(row.id)">解封</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" :page-size="10" @current-change="loadUsers" background layout="prev, pager, next" />
  </div>
</template>
```

- [ ] **Step 2: 更新路由和菜单**

路由追加：
```typescript
{ path: 'users', name: 'users', component: () => import('../views/UserManageView.vue') },
```

菜单追加：
```typescript
{ path: '/users', title: '用户管理' },
```

- [ ] **Step 3: 验证编译**

```bash
cd /Users/cyrene/Dev/shigui/admin-web && npm run build
```
预期：构建成功。

- [ ] **Step 4: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add admin-web/
git -C /Users/cyrene/Dev/shigui commit -m "feat: admin-web user management page"
```

---

### Task 6: 端到端验证

- [ ] **Step 1: 运行全部后端测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test
```
预期：41 tests PASS。

- [ ] **Step 2: 管理端构建**

```bash
cd /Users/cyrene/Dev/shigui/admin-web && npm run build
```
预期：构建成功。

- [ ] **Step 3: 重启 IDEA 后端**

- [ ] **Step 4: curl 冒烟测试**

```bash
ADMIN_TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/admin/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data'])")

# 审核列表
curl -s "http://127.0.0.1:8080/api/admin/posts?status=PENDING_AUDIT" -H "satoken: $ADMIN_TOKEN" | python3 -m json.tool

# 审核通过（假设有 id=1 的待审核单据）
curl -s -X POST "http://127.0.0.1:8080/api/admin/posts/1/approve" -H "satoken: $ADMIN_TOKEN" | python3 -m json.tool

# 用户列表
curl -s "http://127.0.0.1:8080/api/admin/users" -H "satoken: $ADMIN_TOKEN" | python3 -m json.tool

# 封禁用户
curl -s -X PUT "http://127.0.0.1:8080/api/admin/users/1/ban" -H "satoken: $ADMIN_TOKEN" | python3 -m json.tool
```

- [ ] **Step 5: 浏览器验证**

管理端登录后：
- 侧栏菜单有"内容审核"和"用户管理"
- 内容审核页能看到待审核单据，点击通过/删除生效
- 用户管理页能看到用户列表，封禁/解封生效

- [ ] **Step 6: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add docs/superpowers/plans/2026-05-13-sprint-4-content-audit.md
git -C /Users/cyrene/Dev/shigui commit -m "feat: Sprint 4 complete - content audit and user management"
```

---

## 自查

- 规格覆盖: 审核列表/通过/删除 → Task 2，用户列表/封禁/解封 → Task 3，管理端页面 → Task 4+5，audit_record 日志 → Task 1
- 占位符扫描: 无 TBD/TODO，所有步骤有完整代码
- 类型一致性: Controller 方法签名与 Service 一致，AdminController 新依赖注入与 Task 2/3 的注入声明一致
  - `LostFoundPostService.listPublic` 返回 `Page<PostResponse>`，但审核 API 返回原始 `Page<LostFoundPost>`（管理员需要看所有字段包括 privateFeature）
  - 现有 AdminController 的 login 依赖为 `AdminUserService`，Task 2 追加 `LostFoundPostService` + `AuditRecordService`，Task 3 再追加 `AppUserService`

---

### 验证清单

- [ ] 后端 41 个测试全部通过
- [ ] `npm run build` 成功
- [ ] `GET /api/admin/posts` 返回待审核列表
- [ ] `POST /api/admin/posts/{id}/approve` 审核通过
- [ ] `DELETE /api/admin/posts/{id}` 删除 + 记录原因
- [ ] `GET /api/admin/users` 返回用户列表
- [ ] `PUT /api/admin/users/{id}/ban` 封禁
- [ ] `PUT /api/admin/users/{id}/unban` 解封
- [ ] 管理端内容审核页完整可用
- [ ] 管理端用户管理页完整可用
