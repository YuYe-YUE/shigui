# Sprint 6: AI 管理员辅助认领与归还闭环设计

## 背景

拾归的真实校园场景里，拾捡者经常只是把物品交到保卫处、宿管处或失物招领处，不一定愿意继续承担核验、沟通和交还责任。因此 Sprint 6 不再把招领发布者设计成认领流程的核心审核人，而是把流程改为：

- 失主提交认领申请。
- AI 管理员先做私密特征预审。
- 高置信度结果自动处理。
- 低置信度或模糊结果交给人工管理员复核。
- 通过后失主看到暂存地点，自行领取。
- 失主确认收到后完成闭环。

聊天保留为辅助能力，不作为完成归还的必需条件。

## 目标

1. 实现 `FOUND` 招领单的认领申请。
2. 使用 OpenAI-compatible API 做 AI 管理员预审。
3. 支持 AI 高置信自动通过或拒绝。
4. 支持管理员人工通过或拒绝 AI 无法确定的申请。
5. 支持失主在通过后查看暂存地点并确认收到。
6. 小程序提供认领申请、认领记录和确认收到入口。
7. 管理端提供认领审核页面，展示 AI 判断、置信度和原因。

## 非目标

- 不做 WebSocket 实时聊天。
- 不做图片消息。
- 不做管理员仲裁申诉流程。
- 不做复杂相似度阈值后台配置。
- 不让拾捡者成为必需审核节点。

## 角色与职责

### 失主

- 在招领单详情页提交认领申请。
- 填写私密特征答案。
- 在申请通过后查看暂存地点。
- 领取物品后点击“确认收到”。

### 拾捡者

- 发布招领单并填写暂存地点。
- 可以选择参与聊天，但系统不强制其参与认领核验或归还确认。

### AI 管理员

- 对招领单私密特征和认领答案进行预审。
- 返回决策、置信度和简短原因。
- 只在高置信度时自动处理。

### 人工管理员

- 查看所有认领申请。
- 复核 `PENDING_ADMIN_REVIEW` 状态的申请。
- 可查看 AI 原因并手动通过或拒绝。
- 可查看自动通过、自动拒绝的历史记录。

## 状态模型

### `claim_record.status`

- `PENDING_AI_REVIEW`: 认领申请已提交，等待 AI 管理员预审。
- `PENDING_ADMIN_REVIEW`: AI 无法高置信判断，等待人工管理员复核。
- `VERIFIED`: 认领申请已通过，等待失主领取。
- `REJECTED`: 认领申请已拒绝。
- `COMPLETED`: 失主已确认收到，认领闭环完成。

### `lost_found_post.status`

- `MATCHING`: 可被认领。
- `CLAIMING`: 已有进行中的认领申请。
- `RETURNING`: 认领已通过，等待失主领取。
- `COMPLETED`: 物品已归还完成。

### 状态流

```text
MATCHING
  -> 用户提交认领申请
CLAIMING
  -> AI 高置信通过或管理员通过
RETURNING
  -> 失主确认收到
COMPLETED
```

拒绝流程：

```text
CLAIMING
  -> AI 高置信拒绝或管理员拒绝
MATCHING
```

## AI 管理员设计

### 输入

后端调用 OpenAI-compatible Chat Completions API，发送：

- 招领单标题、物品名、品类。
- 招领单描述。
- 招领单私密特征 `privateFeature`。
- 认领人填写的答案 `privateFeatureAnswer`。
- 校区、地点、事件时间。
- 可选的匹配记录分数和原因。

### 输出

AI 必须返回 JSON：

```json
{
  "decision": "APPROVE",
  "confidence": 0.92,
  "reason": "答案与私密特征高度一致"
}
```

`decision` 只允许：

- `APPROVE`
- `REJECT`
- `NEEDS_REVIEW`

### 自动处理阈值

- `APPROVE` 且 `confidence >= 0.85`: 自动通过。
- `REJECT` 且 `confidence >= 0.85`: 自动拒绝。
- 其他情况: 转人工管理员审核。

### 隐私约束

- AI 输入可以包含 `privateFeature` 和认领答案。
- AI 输出原因不得复述私密特征原文。
- 写入 `claim_record` 的 AI 原因需要后端做基础脱敏。
- 小程序只向认领人展示简短结果原因，不展示招领单 `privateFeature`。
- 管理端可查看 `privateFeature` 和认领答案，用于人工复核。

## 后端 API

### 用户端认领

`POST /api/claims`

请求：

```json
{
  "postId": 1,
  "privateFeatureAnswer": "卡套有蓝色贴纸"
}
```

行为：

- 只允许认领 `FOUND` 单。
- 单据必须是 `MATCHING`。
- 认领人不能是招领单发布者本人。
- 封禁用户不能申请。
- 同一张招领单同一时间只允许一个进行中的认领。
- 创建 claim 后触发 AI 管理员预审。

`GET /api/claims/mine`

返回当前用户发起的认领申请。

`PUT /api/claims/{id}/confirm-receive`

行为：

- 只有认领申请人可调用。
- 只允许 `VERIFIED` 状态的 claim。
- claim 变为 `COMPLETED`。
- post 变为 `COMPLETED`。

### 管理端认领审核

`GET /api/admin/claims`

参数：

- `page`
- `size`
- `status` 可选

返回字段：

- claim id
- post id
- 招领单标题、物品名、品类、校区、地点、暂存地点
- 招领单私密特征
- 认领答案
- 申请人 id
- AI 决策、置信度、原因
- claim 状态
- 创建时间

`PUT /api/admin/claims/{id}/approve`

行为：

- 仅管理员可调用。
- `PENDING_ADMIN_REVIEW` 或 `PENDING_AI_REVIEW` 可通过。
- claim 变为 `VERIFIED`。
- post 变为 `RETURNING`。

`PUT /api/admin/claims/{id}/reject`

请求：

```json
{
  "reason": "答案无法证明为物主"
}
```

行为：

- 仅管理员可调用。
- claim 变为 `REJECTED`。
- post 回到 `MATCHING`。

## 数据库字段补充

现有 `claim_record` 表已有基础字段：

- `post_id`
- `claimant_user_id`
- `private_feature_answer`
- `status`

Sprint 6 增补字段：

- `ai_decision VARCHAR(32)`
- `ai_confidence DECIMAL(5,4)`
- `ai_reason TEXT`
- `admin_reason VARCHAR(512)`
- `verified_at DATETIME`
- `completed_at DATETIME`

## 小程序设计

### 详情页

对 `FOUND` 且 `MATCHING` 的单据显示“申请认领”按钮。

点击后弹出输入框或进入认领表单页，提交 `privateFeatureAnswer`。

提交后按返回状态展示：

- 自动通过: 显示“认领已通过，请前往暂存地点领取”。
- 自动拒绝: 显示“认领未通过”。
- 等待人工审核: 显示“等待管理员审核”。

### 认领记录页

从“我的”页进入。

展示当前用户发起的认领申请：

- 待 AI/人工审核
- 已通过，显示暂存地点和“确认收到”
- 已拒绝
- 已完成

### 聊天

聊天仍可保留在详情页或认领记录页，但不是完成流程的必需步骤。Sprint 6 只要求 HTTP 拉取和发送消息，不要求实时推送。

## 管理端设计

新增“认领审核”页面。

表格展示：

- 招领单信息
- 申请人
- 私密特征
- 认领答案
- AI 判断
- AI 置信度
- AI 原因
- 当前状态
- 操作按钮

操作：

- 对待人工审核的申请执行“通过”或“拒绝”。
- 自动处理的申请只读展示，保留审计线索。

## 权限规则

- 封禁用户不能申请认领、确认收到、创建聊天、发送消息。
- 普通用户只能查看自己的认领申请。
- 管理员可查看所有认领申请。
- 管理员可通过或拒绝待审核申请。
- 失主确认收到后，其他人不能再认领该单据。

## 测试策略

### 后端

- `POST /api/claims` 成功创建认领申请。
- 非 `FOUND` 单不能认领。
- 非 `MATCHING` 单不能认领。
- 发布者本人不能认领自己的招领单。
- 同一单据已有进行中 claim 时不能重复认领。
- AI 高置信通过时 claim 自动 `VERIFIED`，post 进入 `RETURNING`。
- AI 高置信拒绝时 claim 自动 `REJECTED`，post 回到 `MATCHING`。
- AI 低置信时 claim 进入 `PENDING_ADMIN_REVIEW`。
- 管理员通过/拒绝接口权限正确。
- 失主确认收到后 claim/post 都进入完成态。

### 小程序

- 详情页能提交认领答案。
- 认领记录页能展示不同状态。
- 通过后能看到暂存地点。
- 确认收到后状态更新为完成。

### 管理端

- 认领审核列表能加载。
- AI 决策、置信度、原因展示正确。
- 管理员可通过/拒绝待审核申请。

## 验收清单

- 后端新增认领 API 并测试通过。
- AI 管理员预审可真实调用 OpenAI-compatible API。
- 低置信结果进入人工审核。
- 管理端能审核认领申请。
- 小程序能申请认领、查看认领记录、确认收到。
- 暂存地点只在认领通过后对认领人展示。
- `./mvnw test` 通过。
- `admin-web npm run build` 通过。
