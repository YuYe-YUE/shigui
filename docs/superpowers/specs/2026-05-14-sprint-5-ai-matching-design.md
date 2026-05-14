# Sprint 5: AI 智能匹配 + 通知提醒 — 设计

## 目标

S5 实现“审核通过后自动匹配”的闭环：管理员审核通过一条单据后，系统将其放入 `MATCHING` 匹配池，自动查找相反类型候选单据，调用兼容 OpenAI 格式的大模型 API 做语义匹配，写入 `match_record`，并给双方生成 `notification`。用户可以在小程序查看自己的匹配结果和通知。

## 当前基线

- S1-S4 已完成微信登录、发布单据、公开列表/详情、管理端审核和用户管理。
- `lost_found_post.status=MATCHING` 表示审核通过并进入匹配池。
- `match_record` 和 `notification` 数据表已存在。
- `match_record` 当前缺少匹配理由字段，S5 需要新增 `reason TEXT`。
- 公开列表和详情只返回 `PostResponse`，不返回用户信息。

## 范围

包含：

- 审核通过后自动触发匹配。
- 后端规则预筛候选单据。
- 调用 OpenAI-compatible Chat Completions API 做语义匹配。
- 默认发送 `privateFeature` 给 AI 参与匹配。
- 写入 `match_record(score, reason)`。
- 给 lost/found 双方生成 `notification`。
- 新增 `GET /api/matches/mine`。
- 新增 `GET /api/notifications`。
- 小程序“匹配提醒/我的匹配”入口对接。
- 后端全量测试包含真实 API 集成测试。

不包含：

- S6 认领流程。
- S6 匿名聊天权限和消息发送。
- 微信模板消息推送。
- 图片上传或图片识别。
- 定时批处理匹配。

## 数据库变更

`match_record` 新增匹配理由：

```sql
ALTER TABLE match_record ADD COLUMN reason TEXT COMMENT 'AI 匹配理由' AFTER score;
```

对应初始化脚本中的 `match_record` 表也同步新增：

```sql
reason TEXT COMMENT 'AI 匹配理由',
```

`reason` 存储 AI 给出的简短解释，但必须经过服务端脱敏处理，不能原样包含 `privateFeature`。

## 后端模块

新增实体和 Mapper：

- `MatchRecord`
- `Notification`
- `MatchRecordMapper`
- `NotificationMapper`

新增 Service：

- `MatchRecordService`
- `NotificationService`
- `AiMatchClient`

`MatchRecordService` 负责匹配主流程：

```text
generateMatchesForPost(postId)
  1. 读取目标单据
  2. 确认目标单据 status=MATCHING 且未删除
  3. 查询相反类型、MATCHING、未删除候选
  4. 规则预筛 Top N
  5. 调用 AiMatchClient
  6. 合并 AI 结果和规则兜底
  7. score 达阈值则写 match_record
  8. 给双方写 notification
```

`AdminPostService.approvePost()` 在状态更新为 `MATCHING` 且审核日志写入成功后调用 `MatchRecordService.generateMatchesForPost(postId)`。

## AI API 接入

API 采用 OpenAI-compatible Chat Completions 格式：

```text
POST {AI_MATCH_BASE_URL}/chat/completions
Authorization: Bearer {AI_MATCH_API_KEY}
Content-Type: application/json
```

配置项：

```properties
ai.match.enabled=true
ai.match.base-url=${AI_MATCH_BASE_URL}
ai.match.api-key=${AI_MATCH_API_KEY}
ai.match.model=${AI_MATCH_MODEL}
ai.match.timeout-seconds=30
ai.match.include-private-feature=true
ai.match.max-candidates=20
ai.match.max-results=5
ai.match.threshold=0.70
```

发送给 AI 的字段：

- `id`
- `postType`
- `title`
- `itemName`
- `itemCategory`
- `description`
- `privateFeature`
- `campusArea`
- `locationName`
- `eventTime`

不发送给 AI 的字段：

- `userId`
- `openid`
- `nickname`
- 聊天记录
- 认领答案
- 管理端审核信息

`privateFeature` 默认发送给 AI，用于提升匹配准确率。它只允许用于服务端匹配判断，不能进入通知正文，不能返回给普通用户，也不能原样写入 `match_record.reason`。

## AI 请求格式

后端用 `system` 消息约束模型：

```text
你是校园失物招领匹配助手。请判断目标单据和候选单据是否可能描述同一个物品。
只返回 JSON，不要返回 Markdown。
score 范围为 0 到 1。
reason 用中文简短说明匹配依据，但不要复述私密特征原文。
```

`user` 消息包含目标单据和候选单据数组。

要求 AI 返回：

```json
{
  "matches": [
    {
      "candidatePostId": 12,
      "matched": true,
      "score": 0.86,
      "reason": "两条单据都提到南校园、校园卡，时间相差较近，地点接近。"
    }
  ]
}
```

解析规则：

- `candidatePostId` 必须来自输入候选列表。
- `score` 小于 0 时按 0 处理，大于 1 时按 1 处理。
- `matched=false` 的结果不写入 `match_record`。
- `score < threshold` 的结果不写入 `match_record`。
- JSON 解析失败、API 超时、网络失败时，使用规则评分兜底。

## 规则预筛

规则预筛用于缩小候选范围和提供兜底分数。满分 `1.0`：

```text
同校区: 0.20
同品类: 0.25
时间接近: 0.20
标题/物品名关键词相似: 0.20
私密特征粗略相似: 0.15
```

时间分：

```text
0-1 天: 0.20
2-3 天: 0.12
4-7 天: 0.06
超过 7 天: 0
```

候选先按规则分排序，取 `ai.match.max-candidates` 条发送给 AI。若 AI 不可用，规则分达到阈值的候选也可以写入匹配记录，但 reason 使用规则解释生成，不包含 `privateFeature` 原文。

## 去重与通知

同一对 lost/found 单据只保留一条匹配记录：

```text
lost_post_id + found_post_id 唯一
```

目标单据可能是 `LOST`，也可能是 `FOUND`。写库前统一转换：

```text
lostPostId = LOST 类型单据 id
foundPostId = FOUND 类型单据 id
```

每次审核通过最多写入 `ai.match.max-results` 条匹配，避免通知过多。

通知生成规则：

- 给 lost 单据发布者生成一条 `type=MATCH` 通知。
- 给 found 单据发布者生成一条 `type=MATCH` 通知。
- `relatedId` 填 `match_record.id`。
- `title` 使用“发现疑似匹配单据”。
- `content` 包含物品名称、校区、匹配分数和简短理由。
- `content` 不包含 `privateFeature` 原文。

## API 设计

### GET /api/matches/mine

登录后查看与当前用户相关的匹配结果。

返回 `MatchResponse` 分页列表：

```text
id
score
reason
myPost
matchedPost
createdAt
```

查询规则：

- 当前用户发布的 lost/found 单据参与的匹配都返回。
- 不返回 `deleted=1` 的匹配。
- 不返回 `privateFeature`。
- 按 `createdAt DESC` 排序。

### GET /api/notifications

登录后查看当前用户通知。

返回 `NotificationResponse` 分页列表：

```text
id
type
title
content
relatedId
isRead
createdAt
```

查询规则：

- 只返回当前用户自己的通知。
- 不返回 `deleted=1`。
- 默认按 `isRead ASC, createdAt DESC` 排序。

S5 只做通知列表读取，不做已读更新；已读操作可以放到后续小迭代。

## 小程序设计

“我的”页新增或接通两个入口：

- `匹配提醒`：进入通知列表，调用 `GET /api/notifications`。
- `我的匹配`：进入匹配列表，调用 `GET /api/matches/mine`。

通知列表展示：

- 标题
- 内容
- 时间
- 未读/已读状态

匹配列表展示：

- 我的单据
- 疑似匹配单据
- 匹配分数
- 匹配理由
- 点击疑似匹配单据进入详情页

S5 页面只负责查看，不发起认领、不创建聊天。S6 再从匹配卡片进入认领和匿名聊天。

## 真实 API 测试

S5 要求 `./mvnw test` 包含真实 OpenAI-compatible API 集成测试。没有配置 API Key、网络不通、模型不可用或返回格式错误时，全量测试失败。

必需环境变量：

```bash
AI_MATCH_API_KEY=your-key
AI_MATCH_BASE_URL=https://provider.example.com/v1
AI_MATCH_MODEL=model-name
```

真实 API 测试数据规模：

```text
1 条目标单据
8-12 条候选单据
```

候选类型：

- 强匹配：同校区、同品类、描述和私密特征高度接近。
- 中等匹配：同品类但地点或时间有偏差。
- 弱匹配：同校区但品类不同。
- 干扰项：完全无关物品。
- 边界项：描述相似但 `privateFeature` 不同。

测试断言：

- 请求成功发送到 `/chat/completions`。
- 返回合法 JSON。
- 至少识别出 1 条强匹配。
- 强匹配 `score >= 0.70`。
- 明显无关项不能进入 `matched=true` 且高分。
- `reason` 存在。
- `candidatePostId` 必须来自输入候选列表。
- `privateFeature` 会进入 AI 输入。
- `privateFeature` 不会原样出现在 `reason`、`notification.content` 或普通用户响应中。

单元测试仍使用 fake `AiMatchClient` 验证业务逻辑，但它不能替代真实 API 集成测试。

## 错误处理

- AI API 超时：记录日志，使用规则兜底。
- AI 返回非法 JSON：记录日志，使用规则兜底。
- AI 返回不存在的 candidate id：忽略该项。
- 写入重复 pair：跳过，不重复通知。
- 目标单据不是 `MATCHING`：不执行匹配。
- 当前用户查询匹配/通知：只能看到自己的数据。

## 验收标准

- 管理员审核通过一条招领单后，系统能自动匹配已有寻物单。
- 匹配分数达到阈值时，`match_record` 写入 `score` 和 `reason`。
- lost/found 双方都收到 `MATCH` 通知。
- 小程序可以查看通知列表和我的匹配列表。
- 同一 lost/found pair 不重复写入。
- 普通用户响应中不出现 `privateFeature`。
- `./mvnw test` 在配置真实 AI API 环境变量后通过，并实际调用模型。
- `admin-web npm run build` 不受 S5 后端变更影响。

