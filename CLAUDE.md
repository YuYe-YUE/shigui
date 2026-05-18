# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 协作原则

1. 不要假设用户清楚自己想要什么，不清晰时停下来讨论
2. 目标清晰但路径不是最优时，直接建议更好的办法
3. 遇到问题追根因，不打补丁，每个决策要能回答"为什么"
4. 输出说重点，砍掉一切不改变决策的信息

## 项目概述

拾归 (Shi-Gui) — 校园失物招领系统，中山大学第19组课程项目。全部 8 个 Sprint 已完成。

- **用户端**: 微信小程序（12 页面），面向失主和拾捡者
- **管理端**: Web 后台 (Vue 3 + Vite + TypeScript + Element Plus, 5 页面)，面向校方保卫处管理员
- **后端**: 105 测试，BUILD SUCCESS
- 设计文档在 `docs/`、`docs/superpowers/specs/`、`docs/superpowers/plans/`

## 技术栈

- **后端**: Spring Boot 3.5.14 + MyBatis-Plus 3.5.16 + Sa-Token 1.45.0, Java 21, Maven
- **前端(管理端)**: Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router + Axios
- **前端(小程序)**: 原生微信小程序框架
- **数据库**: MySQL 8.0
- **AI**: DeepSeek v4-flash（匹配+认领预审）
- **测试**: JUnit 5 + Mockito (后端 105 tests)

## 命令

后端（在 `backend/` 下）：

```bash
./mvnw compile                         # 编译
./mvnw test                             # 全量测试（105 个）
./mvnw test -Dtest='!OpenAi*'          # 跳过 AI API 测试
./mvnw test -DAI_MATCH_BASE_URL=https://api.deepseek.com -DAI_MATCH_API_KEY=sk-... -DAI_MATCH_MODEL=deepseek-v4-flash
```

Maven Central 网络不通时用 IDEA 打开 `backend/` 直接 `Run BackendApplication`，绕过 `spring-boot:run` 的插件依赖。

管理端（在 `admin-web/` 下）：

```bash
npm run dev     # http://localhost:5173
npm run build
```

小程序用微信开发者工具打开 `miniapp/`，关闭域名校验。

数据库：

```bash
mysql -u root -p < scripts/init_schema.sql
mysql -u root -p < scripts/seed_data.sql
```

管理员：admin / admin123

## 架构

三层：`Controller` (REST, `Result<T>`) → `Service` (接口+实现) → `Mapper` (MyBatis-Plus BaseMapper)

包结构: `com.shigui.{common,config,controller,dto,entity,mapper,service}`

关键规则：
- Sa-Token 拦截 `/api/**`，排除 `login`/`wx-login`/`posts/map`/`posts/**`
- `/api/posts/**` 公开，POST/mine 在 Controller 内手动验登录
- `requireAdmin()`：`loginId >= 10_000_000` 才是管理员，否则 403
- `NotPermissionException` → 403, `NotLoginException` → 401
- `status` 表业务流转（`PENDING_AUDIT`/`MATCHING`/`CLAIMING`/`RETURNING`/`COMPLETED`），删除用 `deleted=1`
- `@TableLogic` 只在 audit_record/match_record/notification/chat_*/claim_record 上用，LostFoundPost 不用（管理员需看已删）
- 封禁用户 (`BANNED`) 禁止发布/认领/聊天/发消息
- `AdminPostService`/`MatchRecordServiceImpl.generateMatchesForPost()`/`ClaimRecordServiceImpl.createClaim()` 使用 `@Transactional`

## 数据库

10 张表：`app_user`, `admin_user`, `lost_found_post`, `audit_record`, `claim_record`, `match_record`, `notification`, `chat_session`, `chat_message`, `system_config`。外加 `post_image`（S8）。

## API 全览

| 端点 | 说明 |
|------|------|
| `POST /api/user/wx-login` | 微信登录 |
| `GET /api/user/me` | 用户信息 |
| `POST /api/admin/login` | 管理员登录 |
| `GET /api/admin/dashboard` | 仪表盘统计 |
| `GET /api/admin/posts` | 审核列表 |
| `GET /api/admin/posts/{id}` | 单据详情 |
| `POST /api/admin/posts/{id}/approve` | 审核通过 |
| `DELETE /api/admin/posts/{id}` | 删除单据 |
| `GET /api/admin/users` | 用户列表 |
| `PUT /api/admin/users/{id}/ban` / `unban` | 封禁/解封 |
| `GET /api/admin/matches` | 匹配列表 |
| `GET /api/admin/claims` | 认领列表 |
| `POST /api/admin/claims/{id}/verify` / `reject` | 认领审核 |
| `POST /api/posts` | 发布单据 |
| `GET /api/posts` | 公开列表 |
| `GET /api/posts/mine` | 我的记录 |
| `GET /api/posts/{id}` | 单据详情 |
| `GET /api/posts/map` | 地图点位 |
| `GET /api/matches/mine` | 我的匹配 |
| `GET /api/notifications` | 通知列表 |
| `POST /api/claims` | 发起认领 |
| `GET /api/claims/mine` | 我的认领 |
| `PUT /api/claims/{id}/confirm-receive` | 确认收到 |
| `POST /api/chat/sessions` | 创建聊天 |
| `GET /api/chat/sessions/{id}/messages` | 消息列表 |
| `POST /api/chat/sessions/{id}/messages` | 发消息 |
| `POST /api/files/upload` | 上传图片 |
| `GET /` | 健康检查 |

## Sprint 完成情况

| S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 |
|----|----|----|----|----|----|----|-----|
| 微信登录 | 发布单据 | 列表筛选 | 内容审核 | 智能匹配 | 认领+聊天 | 地图图钉 | 图片上传 |

全部 105 测试通过（2 个 AI 环境变量跳过），admin-web 构建通过。

## 当前状态

- `backend/`: 105 tests, 11 Controller, 12 Service, 10 Entity, 12 Mapper
- `admin-web/`: 登录/仪表盘/内容审核/匹配结果/用户管理/认领审核 6 页面
- `miniapp/`: 12 页面 + post-card 组件 + 20 图标 + 3 node 测试
- `scripts/`: 建表 + 种子数据（4 用户 + 40 单据含坐标）
