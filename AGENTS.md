# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## 协作原则

1. 不要假设用户清楚自己想要什么，不清晰时停下来讨论
2. 目标清晰但路径不是最优时，直接建议更好的办法
3. 遇到问题追根因，不打补丁，每个决策要能回答"为什么"
4. 输出说重点，砍掉一切不改变决策的信息

## 项目概述

拾归 (Shi-Gui) — 校园失物招领系统，中山大学第19组课程项目。全部 8 个 Sprint 已完成。

- **用户端**: 微信小程序（11 页面），面向失主和拾捡者
- **管理端**: Web 后台 (Vue 3 + Vite + TypeScript + Element Plus, 6 页面)，面向校方保卫处管理员
- **后端**: 112 测试 (2 skipped)，BUILD SUCCESS
- 设计文档在 `docs/`、`docs/superpowers/specs/`、`docs/superpowers/plans/`

## 技术栈

- **后端**: Spring Boot 3.5.14 + MyBatis-Plus 3.5.16 + Sa-Token 1.45.0, Java 21, Maven
- **前端(管理端)**: Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router + Axios
- **前端(小程序)**: 原生微信小程序框架
- **数据库**: MySQL 8.0
- **AI**: DeepSeek v4-flash（匹配+认领预审）
- **测试**: JUnit 5 + Mockito + MockMvc (后端 112 tests)

## 命令

后端（在 `backend/` 下）：

```bash
./mvnw compile                         # 编译
./mvnw test                             # 全量测试（112 个）
./mvnw test -Dtest=ClassName            # 单个测试类
./mvnw test -Dtest='!OpenAi*'          # 跳过 AI API 测试（2 个）
./mvnw test -DAI_MATCH_BASE_URL=https://api.deepseek.com -DAI_MATCH_API_KEY=sk-... -DAI_MATCH_MODEL=deepseek-v4-flash
```

Maven Central 网络不通时用 IDEA 打开 `backend/` 直接 `Run BackendApplication`，绕过 `spring-boot:run` 的插件依赖。

管理端（在 `admin-web/` 下）：

```bash
npm run dev     # http://localhost:5173（Vite 代理 /api → localhost:8080）
npm run build
```

管理员登录密码在前端做 SHA-256 哈希后再发送，后端 `salt:SHA256(salt+SHA256(password))` 双层存储。

小程序用微信开发者工具打开 `miniapp/`，关闭域名校验。

数据库：

```bash
mysql -u root -p < scripts/init_schema.sql
mysql -u root -p < scripts/seed_data.sql
```

管理员：admin / admin123

## 架构

```
小程序/管理端 ──satoken──▶ SaToken ──▶ Controller ──▶ Service ──▶ Mapper ──▶ MySQL
                              │                    │
                         公开端点              @Transactional
                              │                    │
                     requireAdmin()         DeepSeek API
                     (管理员 ID ≥ 10M)       (匹配 + 认领预审)
```

三层：`Controller` (REST, `Result<T>`) → `Service` (接口+实现) → `Mapper` (MyBatis-Plus BaseMapper)。包结构: `com.shigui.{common,config,controller,dto,entity,mapper,service}`

关键规则：
- Sa-Token 拦截 `/api/**`，排除 `login`/`wx-login`/`posts/map`/`posts/**`
- `/api/posts/**` 公开，POST/mine 在 Controller 内手动验登录
- `requireAdmin()`：`loginId >= 10_000_000` 才是管理员，否则 403
- `NotPermissionException` → 403, `NotLoginException` → 401
- `status` 表业务流转（`PENDING_AUDIT`/`MATCHING`/`CLAIMING`/`RETURNING`/`COMPLETED`），删除用 `deleted=1`
- `@TableLogic` 只在 audit_record/match_record/notification/chat_*/claim_record 上用，LostFoundPost 不用（管理员需看已删）
- 认领/聊天 API 在 Service 层校验所有权（`claimantUserId`/`lostUserId`/`foundUserId`），防止水平越权
- 封禁用户 (`BANNED`) 禁止发布/认领/聊天/发消息
- `AdminPostService`/`MatchRecordServiceImpl.generateMatchesForPost()`/`ClaimRecordServiceImpl.createClaim()` 使用 `@Transactional`

## 数据库

10 张表：`app_user`, `admin_user`, `lost_found_post`, `audit_record`, `claim_record`, `match_record`, `notification`, `chat_session`, `chat_message`, `system_config`。外加 `post_image`（S8）。

## Sprint 完成情况

| S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 |
|----|----|----|----|----|----|----|-----|
| 微信登录 | 发布单据 | 列表筛选 | 内容审核 | 智能匹配 | 认领+聊天 | 地图图钉 | 图片上传 |

全部 112 测试通过（2 个 AI 环境变量跳过），admin-web 构建通过。

## 当前状态

- `backend/`: 112 tests (含 1 端到端), 9 Controller, 12 Service, 11 Entity, 10 Mapper
- `admin-web/`: 登录/仪表盘/内容审核/匹配结果/用户管理/认领审核 6 页面
- `miniapp/`: 11 页面 + post-card 组件 + 20 图标 + 3 node 测试
- `scripts/`: 建表 + 种子数据（4 用户 + 40 单据含坐标）
