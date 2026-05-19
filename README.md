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

## 本地部署指南

以下步骤在所有小组成员电脑上均可执行，无需服务器。

### 1. 环境准备

| 依赖 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21+ | `java -version` 确认 |
| Maven | 3.8+ | 项目自带 `mvnw`，无需全局安装 |
| Node.js | 18+ | `node -v` 确认 |
| MySQL | 8.0 | `mysql -V` 确认 |
| 微信开发者工具 | 最新稳定版 | 下载 macOS/Windows 版 |

AI 匹配功能需要 DeepSeek API Key（可选，不影响基本使用）。获取地址：https://platform.deepseek.com

### 2. 克隆项目

```bash
git clone https://github.com/YuYe-YUE/shigui.git
cd shigui
```

### 3. 初始化数据库

```bash
# 按提示输入 MySQL root 密码
mysql -u root -p < scripts/init_schema.sql
mysql -u root -p < scripts/seed_data.sql
```

种子数据包含：
- 管理员账号（admin / admin123）
- 4 个测试用户（端到端测试用）
- 40 条示例单据（含坐标，可直接测试地图功能）

### 4. 启动后端

创建 `backend/src/main/resources/application-local.properties`（已加入 `.gitignore`，不会提交）：

```properties
# MySQL 数据库密码（必填）
spring.datasource.password=你的MySQL密码

# DeepSeek AI 配置（可选，不影响基本使用）
AI_MATCH_BASE_URL=https://api.deepseek.com
AI_MATCH_API_KEY=sk-你的API-Key
AI_MATCH_MODEL=deepseek-v4-flash
```

启动方式二选一：

**A. IDE 启动（推荐）**
用 IntelliJ IDEA 打开 `backend/` 目录，运行 `BackendApplication.java`。

**B. 命令行启动**
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

后端启动后访问 http://localhost:8080，看到 `{"code":200}` 即成功。

### 5. 启动管理端 Web

```bash
cd admin-web
npm install
npm run dev
```

浏览器打开 http://localhost:5173，用 `admin` / `admin123` 登录。

Vite 已配置代理：`/api` 和 `/uploads` 自动转发到 `http://127.0.0.1:8080`，无需额外配置。

### 6. 启动小程序

1. 打开**微信开发者工具**
2. 导入项目 → 选择 `miniapp/` 目录
3. AppID 选择「测试号」或使用现有 AppID `wx58ca83eb30d907e7`
4. 右上角「详情」→「本地设置」→ 勾选 **「不校验合法域名」**

小程序 `app.js` 的 `baseUrl` 已配置为 `http://127.0.0.1:8080`。

### 7. 运行测试

```bash
cd backend
./mvnw test                              # 全量 112 测试
./mvnw test -Dtest='!OpenAi*'            # 跳过 AI API 测试（2 个）
```

预期结果：`Tests run: 112, Failures: 0, Errors: 0, Skipped: 2`

### 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 管理端登录 |
| 测试用户 | — | — | 小程序微信登录即可创建 |

### 常见问题

**Q: 小程序点击登录提示「网络请求失败」**
- 确认后端已启动且端口 8080 未被占用
- 微信开发者工具 → 详情 → 不校验合法域名（必须勾选）

**Q: 管理端登录提示「用户名或密码错误」**
- 确认已执行 `scripts/seed_data.sql`
- 密码前端先做 SHA-256 再发送，后端双层哈希存储

**Q: Maven 编译报错**
- 使用项目自带的 `./mvnw`（不需要全局 Maven）
- IDEA 用户直接 `Run BackendApplication.java`

**Q: 图片上传/显示不出来**
- 确认 `uploads/` 目录存在且有写入权限（后端启动时会自动创建）
- 管理端图片通过 Vite 代理 `/uploads` → 后端 8080

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
