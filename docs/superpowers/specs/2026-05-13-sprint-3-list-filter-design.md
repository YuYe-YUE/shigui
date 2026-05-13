# Sprint 3: 信息筛选 + 记录查看 — 设计

## 目标

让用户能浏览公开单据列表（筛选/搜索/分页）和查看自己的历史记录。将小程序首页从占位页变为可用的列表页。

## 后端变更

### 新增 Service 方法

`LostFoundPostService` 新增：

```java
Page<PostResponse> listPublic(int page, int size, String postType,
    String itemCategory, String campusArea, String keyword);

Page<PostResponse> listMine(Long userId, int page, int size, String postType);
```

- `listPublic`: 只查 `status=MATCHING, deleted=0`，支持 5 个可选筛选参数，`keyword` 用 `LIKE` 匹配 `title`
- `listMine`: 查 `user_id=userId` 且 `deleted=0`，不限 `status`
- 两者按 `published_at DESC` 排序

### 新增 Controller 端点

```
GET /api/posts?page=1&size=10&postType=&itemCategory=&campusArea=&keyword=
    — 公开，加入 Sa-Token 放行列表

GET /api/posts/mine?page=1&size=10&postType=
    — 需登录
```

### Sa-Token 变更

`/api/posts` (GET) 加入 `excludePathPatterns`。

### 测试

- `LostFoundPostServiceTest`: 新增 7 个测试
  - listPublic 只返回 MATCHING
  - listPublic 不返回已删除
  - listPublic keyword 模糊匹配
  - listPublic 组合筛选
  - listMine 只返回当前用户
  - listMine 包含所有状态
  - listMine 不返回 deleted=1
- `LostFoundPostControllerTest`: 新增 3 个测试
  - 公开列表无需登录返回 200
  - 我的记录未登录返回 401
  - 我的记录登录后返回 200

## 小程序变更

### 首页（`pages/index/index.*`）

完全复刻旧项目 UI：
- 吸顶双行头部：搜索框（对接 keyword）+ 地图图标按钮（跳转 map 页）
- 寻物/招领胶囊滑动 Tab：点两次切回全部
- 8 品类横向滚动筛选
- 双列瀑布流（CSS `column-count: 2`）
- post-card 组件渲染每张卡片
- 下拉刷新（`onPullDownRefresh`）重置 page=1
- 触底加载（`onReachBottom`）page++ 追加
- 空数据提示

JS 数据流：
- `loadPosts()` → `GET /api/posts?page=&size=10&postType=&itemCategory=&keyword=`
- 首次加载和切换筛选时 `page=1, posts=[]`
- 触底时 `page+=1, posts=[...posts, ...newPosts]`

### 详情页（`pages/detail/detail.js`）

UI 已有。微调 JS：
- `loadDetail()` 调用 `GET /api/posts/{id}`（S2 已实现）
- "申请认领"按钮 → `wx.showToast({ title: '功能开发中', icon: 'none' })`
- "联系对方"按钮 → 同上

### "我的"页金刚区

`goMyPosts()` 暂弹 toast "功能开发中"，不在 S3 范围内贯通。

## 不在范围

- 认领流程、聊天
- 管理端内容审核
- 地图后端接口
- "我的寻物/招领"入口贯通
- 图片上传
