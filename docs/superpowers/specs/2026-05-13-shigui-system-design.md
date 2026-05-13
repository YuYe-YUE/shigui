# 拾归系统 — 整体设计

> 全新仓库从头开发，技术栈不变。需求基线来自《第19组-拾归系统-整合.md》，范围聚焦核心功能。

## 技术栈

- **后端**: Spring Boot 3.5.14 + MyBatis-Plus 3.5.16 + Sa-Token 1.45.0, Java 21, Maven
- **小程序**: 原生微信小程序（UI 设计复刻现有代码，逻辑代码重写）
- **管理端**: Vue 3 + Vite + TypeScript + Element Plus（最小后台：登录 + 内容审核 + 用户管理）
- **数据库**: MySQL 8.0
- **测试**: JUnit 5 + Mockito (后端), Vitest + @vue/test-utils (管理端), miniprogram-simulate (小程序)

## 功能范围

16 个用例中选取核心子集。管理端内容审核采用“发布后待审核，通过后进入匹配池；违规内容逻辑删除并记录原因”的最小闭环：

| Sprint | 用例 | 说明 |
|--------|------|------|
| S1 | 微信登录认证 | 用户身份基础 |
| S2 | 发布丢失/拾捡单 | 数据入口，含表单校验 |
| S3 | 信息筛选 + 记录查看 | 首页列表/详情页 |
| S4 | 内容审核（管理端） | 管理员查看待审核/全量单据 + 审核通过 + 删除违规 |
| S5 | 智能匹配 + 消息提醒 | 匹配算法 + 通知推送 |
| S6 | 物品验证与认领 + 匿名聊天 | 业务闭环 |
| S7 | 地图图钉展示 | 空间可视化 |

## 数据库设计

10 张表，全部使用逻辑删除 (`deleted TINYINT DEFAULT 0`)，MyBatis-Plus 驼峰转下划线。`status` 只表示业务状态，删除/隐藏统一由 `deleted` 字段表达，避免 `status=DELETED` 与逻辑删除重复。

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `app_user` | 小程序用户 | id, openid, nickname, avatar_url, role, status(NORMAL/BANNED), created_at |
| `admin_user` | 后台管理员 | id, username, password_hash, created_at |
| `lost_found_post` | 失物招领单据 | id, user_id, post_type(LOST/FOUND), title, item_name, item_category, description, private_feature, campus_area, location_name, longitude, latitude, storage_location, event_time, status(PENDING_AUDIT/MATCHING/CLAIMING/RETURNING/COMPLETED), published_at |
| `audit_record` | 内容审核操作日志 | id, admin_id, post_id, action(APPROVE/DELETE), reason, created_at |
| `claim_record` | 认领申请与核验 | id, post_id, claimant_user_id, private_feature_answer, status, created_at |
| `match_record` | 智能匹配结果 | id, lost_post_id, found_post_id, score, created_at |
| `notification` | 站内/微信通知 | id, user_id, type, title, content, related_id, is_read, created_at |
| `chat_session` | 匿名聊天会话 | id, post_id, lost_user_id, found_user_id, status(ACTIVE/CLOSED), created_at |
| `chat_message` | 聊天消息 | id, session_id, sender_user_id, content, msg_type(TEXT/IMAGE), created_at |
| `system_config` | 系统配置 KV | id, config_key, config_value, description, updated_at |

### 单据状态流转

```
PENDING_AUDIT → MATCHING → CLAIMING → RETURNING → COMPLETED
                 ↓                         ↓
        deleted=1 + audit_record     (管理员可随时删除)
```

- `PENDING_AUDIT`: 发布后待审核（管理端可查看并决定保留或删除）
- `MATCHING`: 审核通过后进入匹配池，可被筛选、匹配、地图展示
- `CLAIMING`: 有人发起认领
- `RETURNING`: 核验通过，等待归还
- `COMPLETED`: 归还完成
- 删除不是业务状态：管理员删除时设置 `deleted=1`，并写入 `audit_record(action=DELETE, reason=...)`
- 审核通过时写入 `audit_record(action=APPROVE)`，并将单据从 `PENDING_AUDIT` 更新为 `MATCHING`

## 后端设计

### 包结构

```
com.shigui/
├── BackendApplication.java
├── common/
│   ├── Result.java              # 统一响应 {code, message, data}
│   └── GlobalExceptionHandler.java
├── config/
│   ├── SaTokenConfig.java       # 鉴权拦截器
│   ├── CorsConfig.java          # CORS 过滤器
│   └── MybatisPlusConfig.java   # 分页插件 + 逻辑删除
├── controller/
│   ├── AppUserController.java   # /api/user/*
│   ├── AdminController.java     # /api/admin/*
│   ├── LostFoundPostController.java  # /api/posts/*
│   ├── ClaimRecordController.java    # /api/claims
│   ├── MatchRecordController.java    # /api/matches
│   ├── NotificationController.java   # /api/notifications
│   ├── ChatController.java          # /api/chat/*
│   └── SystemConfigController.java  # /api/config
├── entity/                      # 10 个实体
├── dto/                         # Request/Response DTO
├── service/ + impl/             # 业务接口 + 实现
└── mapper/                      # MyBatis-Plus Mapper 接口
```

### API 端点

S1-S7 每次 Sprint 新增的端点：

**S1 微信登录:**
- `POST /api/user/wx-login` — 微信登录（公开）
- `GET /api/user/me` — 获取当前用户信息

**S2 发布单据:**
- `POST /api/posts` — 发布新单据（登录后）
- `GET /api/posts/{id}` — 查看单据详情

**S3 信息筛选:**
- `GET /api/posts?page=&size=&postType=&itemCategory=&campusArea=` — 分页筛选

**S4 内容审核:**
- `GET /api/admin/posts?page=&size=&status=` — 管理员查看全量单据
- `POST /api/admin/posts/{id}/approve` — 管理员审核通过，单据进入匹配池
- `DELETE /api/admin/posts/{id}` — 管理员删除违规单据 (body: {reason})
- `POST /api/admin/login` — 管理员登录（公开）
- `GET /api/admin/users?page=&size=&status=` — 管理员查看小程序用户
- `PUT /api/admin/users/{id}/ban` — 封禁用户，禁止继续发布/认领/聊天
- `PUT /api/admin/users/{id}/unban` — 解封用户

**S5 智能匹配:**
- `GET /api/matches/mine` — 查看我的匹配结果
- `GET /api/notifications` — 获取通知列表

匹配 MVP 规则：
- 只匹配状态为 `MATCHING`、未删除、类型相反的单据（`LOST` 对 `FOUND`）
- 分数由同校区、同品类、时间接近、标题/物品名关键词相似组成
- 分数达到阈值后写入 `match_record`，并为双方生成 `notification`
- 同一对 lost/found 单据只保留一条匹配记录，避免重复提醒

**S6 认领与聊天:**
- `POST /api/claims` — 发起认领申请
- `PUT /api/claims/{id}/verify` — 拾捡者核验
- `PUT /api/claims/{id}/confirm-return` — 确认归还
- `PUT /api/claims/{id}/confirm-receive` — 确认收到
- `POST /api/chat/sessions` — 创建聊天会话
- `GET /api/chat/sessions/{id}/messages` — 获取消息
- `POST /api/chat/sessions/{id}/messages` — 发送消息

聊天权限：
- 只有单据发布者、认领申请人、匹配结果双方可以创建或访问对应会话
- 获取消息和发送消息都必须校验当前用户属于会话双方之一
- 匿名聊天只在前端隐藏真实昵称；后端仍保存真实 `sender_user_id` 以便审计和风控

**S7 地图:**
- `GET /api/posts/map` — 获取地图点位（公开脱敏）

地图返回字段限制：
- 只返回 `id`, `postType`, `itemCategory`, `campusArea`, `locationName`, `longitude`, `latitude`, `eventTime`
- 不返回 `description`, `privateFeature`, `storageLocation`, 用户信息、聊天或认领信息
- 只返回 `MATCHING` 且未删除的单据

### Sa-Token 配置

拦截 `/api/**`，排除公开端点：
```
/api/user/wx-login
/api/admin/login
/api/posts/map
```

封禁用户校验：
- `app_user.status=BANNED` 的用户仍可登录查看自己的历史记录
- 发布、认领、创建聊天、发送消息等写操作必须拒绝封禁用户

### 测试策略

每个 Sprint 交付前：
- **Service 层**: JUnit 5 + Mockito，每个 Service 方法至少 1 个正常路径 + 1 个异常路径
- **Controller 层**: @WebMvcTest + MockMvc，验证 HTTP 状态码和响应体结构
- **Mapper 层**: @MybatisPlusTest + H2 内存数据库，验证 SQL 逻辑

## 管理端设计

### 页面

| 路径 | 页面 | 功能 |
|------|------|------|
| `/login` | 登录页 | 管理员账号密码登录 |
| `/dashboard` | 仪表盘 | 概览统计卡片 |
| `/posts` | 内容审核 | 待审核/全量单据列表 + 查看详情 + 审核通过 + 删除违规 + 填写原因 |
| `/users` | 用户管理 | 小程序用户列表 + 封禁/解封 |

### 路由与状态

- Vue Router: 登录页独立路由，其余页面在 MainLayout（左侧导航 + 右侧内容）
- Pinia: `useAuthStore` 管理 login/logout/token
- Axios: 统一 baseURL `http://127.0.0.1:8080`，请求拦截器注入 satoken

### 测试策略

- 组件渲染测试：@vue/test-utils mount 验证关键 UI 元素
- Store 测试：验证 login/logout 状态变更

## 小程序设计

### 页面（7 个，复刻现有 UI 设计）

| 页面 | 路由 | 说明 |
|------|------|------|
| 首页 | `pages/index/index` | 瀑布流列表 + 寻物/招领胶囊切换 + 品类筛选 + 地图入口 |
| 发布入口 | `pages/publish/publish` | 二分页选择寻物/招领 |
| 发布表单 | `pages/publish-form/publish-form` | 物品信息表单填写 |
| 详情页 | `pages/detail/detail` | Hero 区域 + 信息卡片 + FAB 操作栏 |
| 聊天页 | `pages/chat/chat` | 匿名左右气泡聊天 |
| 地图页 | `pages/map/map` | 微信原生 map + Bottom Sheet 弹窗 |
| 我的 | `pages/mine/mine` | 用户信息 + 快捷操作 + 菜单 |

### 组件

| 组件 | 说明 |
|------|------|
| `post-card` | 杂志风卡片：图片占位区 + 类型标签 + 标题 + 地点 + 时间 |

### 色彩体系

- 主色: `#00573D` (中大绿) / `#2E7D32`
- 寻物: `#FF9800` (橙)
- 招领: `#4CAF50` (绿)
- 背景: `#F8F9FA`

### 数据流

- `app.globalData`: token + baseUrl
- 请求鉴权: `header.satoken`
- 登录: `wx.login` → `POST /api/user/wx-login`

### 测试策略

- 页面/组件单元测试：miniprogram-simulate 模拟渲染
- 关键流程测试：登录 → 发布 → 列表筛选 → 详情 → 认领

## 开发顺序（敏捷垂直切片）

每个 Sprint 交付全栈可运行增量：

```
S1 → S2 → S3 → S4 → S5 → S6 → S7
```

每个 Sprint 包含：
1. 数据库迁移（如需要）
2. 后端 Entity/Mapper/Service/Controller + 测试
3. 管理端页面（S1, S4）+ 测试
4. 小程序页面 + 测试
5. 验证通过后 Git commit
