# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 协作原则

1. 不要假设用户清楚自己想要什么，不清晰时停下来讨论
2. 目标清晰但路径不是最优时，直接建议更好的办法
3. 遇到问题追根因，不打补丁，每个决策要能回答"为什么"
4. 输出说重点，砍掉一切不改变决策的信息

## 项目概述

拾归 (Shi-Gui) — 校园失物招领系统，中山大学课程项目。全新仓库，从头开发。

- **用户端**: 微信小程序，面向失主和拾捡者
- **管理端**: Web 后台 (Vue 3 + Vite + TypeScript)，面向校方保卫处管理员
- 完整设计文档在 `docs/superpowers/specs/2026-05-13-shigui-system-design.md`
- Sprint 计划在 `docs/superpowers/plans/`

## 技术栈

- **后端**: Spring Boot 3.5.14 + MyBatis-Plus 3.5.16 + Sa-Token 1.45.0, Java 21, Maven
- **前端(管理端)**: Vue 3 + Vite + TypeScript + Element Plus
- **前端(小程序)**: 原生微信小程序框架，UI 复刻自旧项目 `~/Dev/shi-gui/miniapp/`
- **数据库**: MySQL 8.0
- **测试**: JUnit 5 + Mockito (后端), Vitest + @vue/test-utils (管理端)

## 命令

后端命令在 `backend/` 目录下执行：

```bash
./mvnw compile              # 编译
./mvnw test                  # 运行全部测试（当前 26 个）
./mvnw test -Dtest=ClassName # 运行单个测试类
```

后端运行需要数据库已初始化。Maven Central 可能网络不通，用 IDEA 打开 `backend/` 直接运行 `BackendApplication.java` 即可绕过 `spring-boot:run` 的插件依赖问题。

管理端在 `admin-web/` 目录下：

```bash
npm run dev    # 开发服务器 (http://localhost:5173)
npm run build  # 构建
```

小程序用微信开发者工具打开 `miniapp/` 目录。

数据库初始化：

```bash
mysql -u root -pHang0611@ -e "DROP DATABASE IF EXISTS shi_gui; CREATE DATABASE shi_gui DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -pHang0611@ < scripts/init_schema.sql
mysql -u root -pHang0611@ < scripts/seed_data.sql
```

## 架构

三层分层架构：

1. **Controller** — RESTful API，统一 `Result<T>` 响应体
2. **Service** — 业务逻辑层，接口与实现分离（`service/` 接口 + `service/impl/` 实现）
3. **Mapper** — MyBatis-Plus BaseMapper，驼峰转下划线自动映射

包结构: `com.shigui.{common,config,controller,dto,entity,mapper,service}`

关键约束：
- Sa-Token 鉴权拦截 `/api/**`，公开端点需显式排除：`/api/user/wx-login`、`/api/admin/login`、`/api/posts/map`
- 所有表使用逻辑删除 (`deleted TINYINT DEFAULT 0`)
- 持久层实体与 API 之间使用 DTO 隔离（`CreatePostRequest` / `PostResponse` 等）
- `status` 只表示业务流转（`PENDING_AUDIT`/`MATCHING`/`CLAIMING`/`RETURNING`/`COMPLETED`），删除统一用 `deleted=1`
- 封禁用户校验在 Service 层：`app_user.status=BANNED` 时禁止发布/认领/聊天/发消息

## 数据库

10 张表：`app_user`, `admin_user`, `lost_found_post`, `audit_record`, `claim_record`, `match_record`, `notification`, `chat_session`, `chat_message`, `system_config`

## 当前 API

| 端点 | Sprint | 说明 |
|------|--------|------|
| `POST /api/user/wx-login` | S1 | 微信登录（公开） |
| `GET /api/user/me` | S1 | 当前用户信息 |
| `POST /api/admin/login` | S1 | 管理员登录（公开） |
| `POST /api/posts` | S2 | 发布单据 |
| `GET /api/posts/{id}` | S2 | 查看单据详情 |
| `GET /` | — | 健康检查 |

## 开发方法

敏捷垂直切片，按 Sprint 逐个交付全栈可运行增量。7 个 Sprint：

| Sprint | 用例 | 状态 |
|--------|------|------|
| S1 | 微信登录认证 | ✅ |
| S2 | 发布丢失/拾捡单 | ✅ |
| S3 | 信息筛选 + 记录查看 | 待开始 |
| S4 | 内容审核（管理端） | 待开始 |
| S5 | 智能匹配 + 消息提醒 | 待开始 |
| S6 | 物品验证与认领 + 匿名聊天 | 待开始 |
| S7 | 地图图钉展示 | 待开始 |

每个 Sprint 完成：后端 API + 测试 → 前端页面/组件 → 验证 → commit。

## 当前状态

- `backend/`: 26 个测试全过，6 个 Controller、4 个 Service、4 个 Entity、4 个 Mapper、3 个 Config 类
- `admin-web/`: 登录页、仪表盘布局、路由/Auth Store/Axios 封装就绪，内容审核和用户管理页面待实现
- `miniapp/`: 7 页面 + post-card 组件 + 17 个 SVG 图标，中大绿（#00573D）杂志卡片风。首页为占位，detail/chat/map 页面 UI 和 JS 已完整对接后端 API
- `scripts/`: 建表 + 种子数据（管理员 admin/admin123，2 个测试用户）
