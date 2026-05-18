# 拾归 (Shi-Gui) — 校园失物招领系统

中山大学第19组课程项目。基于地图定位与 AI 智能匹配的校园失物招领平台。

## 技术栈

| 层 | 技术 |
|------|------|
| 后端 | Spring Boot 3.5 + MyBatis-Plus 3.5 + Sa-Token 1.45, Java 21, Maven |
| 管理端 | Vue 3 + Vite + TypeScript + Element Plus + Pinia |
| 小程序 | 原生微信小程序框架 |
| 数据库 | MySQL 8.0 |
| AI | DeepSeek v4-flash（匹配 + 认领预审） |
| 测试 | JUnit 5 + Mockito + MockMvc（112 测试） |

## 功能

### 用户端（微信小程序，11 页面）

- 微信一键登录
- 首页瀑布流浏览，寻物/招领胶囊切换，8 品类筛选，关键词搜索
- 发布寻物/招领单，支持图片上传（最多 3 张）和地图标注捡拾位置
- 单据详情页
- 申请认领——填写私密特征，AI 预审后管理员审核
- 认领记录，确认收到物品
- 匿名聊天（失主↔拾捡者），角色脱敏
- 我的匹配——AI 智能匹配结果 + 匹配度
- 匹配提醒——站内通知 + 未读红点
- 地图找招领——展示有坐标的招领单 marker

### 管理端（Web 后台，6 页面）

- 仪表盘——注册用户/匹配中/今日发布/成功认领/匹配记录实时统计
- 内容审核——待审核/全部 Tab，查看详情，审核通过/删除
- 匹配结果——查看所有 AI 匹配记录
- 认领审核——AI 预审结果，通过/拒绝
- 用户管理——封禁/解封

### AI 能力

- **智能匹配**：审核通过后自动触发。规则预筛（校区+品类+时间+文本）后调用 DeepSeek v4-flash 语义排序，AI 失败时规则兜底。生成匹配记录和双方通知
- **认领预审**：失主填写私密特征后 AI 判断是否匹配，支持自动通过/拒绝/转人工

## 架构

```
小程序/管理端 ──satoken──▶ SaToken ──▶ Controller ──▶ Service ──▶ Mapper ──▶ MySQL
                              │                    │
                         公开端点              @Transactional
                              │                    │
                     requireAdmin()         DeepSeek API
                     (管理员 ID ≥ 10M)       (匹配 + 认领预审)
```

三层分层：Controller（REST）→ Service（业务）→ Mapper（数据访问）。统一 `Result<T>` 响应体。`deleted` 逻辑删除，`status` 业务状态流转。

## 快速开始

### 前提

- Java 21, Maven, Node.js, MySQL 8.0
- 微信开发者工具
- DeepSeek API Key（AI 匹配功能需要）

### 数据库

```bash
mysql -u root -p < scripts/init_schema.sql
mysql -u root -p < scripts/seed_data.sql
```

### 后端

```bash
cd backend
# 配置 application-local.properties（数据库密码 + AI API Key）
# IDEA 打开 backend/，运行 BackendApplication.java
./mvnw test   # 112 测试
```

### 管理端

```bash
cd admin-web
npm install
npm run dev    # http://localhost:5173
```

管理员账号：`admin` / `admin123`

### 小程序

微信开发者工具打开 `miniapp/` 目录，关闭域名校验。

## 项目结构

```
shigui/
├── backend/                     # Spring Boot 后端 (80 Java 文件)
│   └── src/main/java/com/shigui/
│       ├── common/              # Result, GlobalExceptionHandler
│       ├── config/              # SaToken, CORS, MyBatisPlus, AI Props
│       ├── controller/          # 9 个 Controller, 30+ API 端点
│       ├── dto/                 # 19 个请求/响应 DTO
│       ├── entity/              # 11 个数据库实体
│       ├── mapper/              # 10 个 MyBatis-Plus Mapper
│       └── service/             # 12 接口 + 12 实现
├── admin-web/                   # Vue 3 管理端 (6 页面)
├── miniapp/                     # 微信小程序 (11 页面 + 1 组件)
├── scripts/                     # 建表 + 种子数据
└── docs/                        # 设计文档 + 测试报告 + 用户手册
```

## 测试

```
Tests run: 112, Failures: 0, Errors: 0, Skipped: 2
```

| 类型 | 数量 | 覆盖 |
|------|------|------|
| Service 单元测试 | 46 | 全部业务逻辑 |
| Controller 集成测试 | 54 | 全部 9 个 Controller |
| 端到端集成测试 | 1 | 发布→审核→匹配→通知全链路 |
| Schema 契约测试 | 1 | 表结构验证 |

跳过的 2 个为 AI API 真实调用测试（需环境变量）。

## 开发团队

中山大学 第19组（7人）

## 文档

- `docs/第19组-拾归系统-软件需求规格说明书.docx`
- `docs/第19组-拾归系统-软件体系结构设计.docx`
- `docs/第19组-拾归系统-用户界面设计.docx`
- `docs/第19组-拾归系统-软件详细设计.md`
- `docs/第19组-拾归系统-测试报告.md`
- `docs/第19组-拾归系统-用户手册.md`
- `docs/第19组-拾归系统-项目总结.md`
