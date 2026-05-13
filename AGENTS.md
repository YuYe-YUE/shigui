# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

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

## 技术栈

- **后端**: Spring Boot 3.5.14 + MyBatis-Plus 3.5.16 + Sa-Token 1.45.0, Java 21, Maven
- **前端(管理端)**: Vue 3 + Vite + TypeScript + Element Plus
- **前端(小程序)**: 原生微信小程序框架
- **数据库**: MySQL 8.0
- **测试**: JUnit 5 + Mockito (后端), Vitest + @vue/test-utils (管理端)

## 命令

后端命令在 `backend/` 目录下执行：

```bash
# 编译
./mvnw compile

# 运行全部测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=ClassName

# 启动应用（需数据库已初始化）
./mvnw spring-boot:run
```

管理端命令在 `admin-web/` 目录下执行：

```bash
npm run dev       # 开发服务器 (http://localhost:5173)
npm run build     # 构建
npm test          # 运行测试
```

小程序用微信开发者工具打开 `miniapp/` 目录。

## 架构

三层分层架构：

1. **Controller** — RESTful API，统一 `Result<T>` 响应体
2. **Service** — 业务逻辑层，接口与实现分离
3. **Mapper** — MyBatis-Plus BaseMapper，驼峰转下划线自动映射

关键约束：
- Sa-Token 鉴权拦截 `/api/**`，公开端点需显式排除
- 所有表使用逻辑删除 (`deleted TINYINT DEFAULT 0`)
- 持久层实体与 API 之间使用 DTO 隔离
- 包结构: `com.shigui.{common,config,controller,dto,entity,mapper,service}`

## 数据库

10 张表：`app_user`, `admin_user`, `lost_found_post`, `audit_record`, `claim_record`, `match_record`, `notification`, `chat_session`, `chat_message`, `system_config`

## 开发方法

敏捷垂直切片，按 Sprint 逐个交付全栈可运行增量。7 个 Sprint：微信登录 → 发布单据 → 信息筛选 → 内容审核 → 智能匹配 → 认领聊天 → 地图图钉。

每个 Sprint 完成：数据库迁移 → 后端 API + 测试 → 前端页面 + 测试 → 验证 → commit。

## 当前状态

项目刚初始化，仅设计文档已提交。尚无代码。
