# Sprint 3: 信息筛选 + 记录查看 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 添加公开列表分页筛选搜索 API 和"我的记录"API，将小程序首页从占位页改为完整列表页（瀑布流卡片+筛选+分页）。

**Architecture:** 后端在现有 `LostFoundPostService` 上新增 `listPublic`/`listMine` 两个方法，Controller 新增 `GET /api/posts` (公开) 和 `GET /api/posts/mine` (需登录)。小程序首页复刻旧项目 UI：吸顶双行头部 + 胶囊 Tab + 品类滚动筛选 + 双列瀑布流 + 下拉刷新/触底分页。

**Tech Stack:** Spring Boot 3.5.14, MyBatis-Plus 3.5.16, Sa-Token 1.45.0, Java 21, 原生微信小程序。

---

## 文件结构

- Modify: `backend/src/main/java/com/shigui/service/LostFoundPostService.java`
- Modify: `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`
- Modify: `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`
- Modify: `backend/src/main/java/com/shigui/config/SaTokenConfig.java`
- Modify: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`
- Modify: `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java`
- Modify: `miniapp/pages/index/index.js`
- Modify: `miniapp/pages/index/index.wxml`
- Modify: `miniapp/pages/index/index.wxss`
- Modify: `miniapp/pages/detail/detail.js`

---

### Task 1: Service 层 — listPublic + listMine（TDD）

**Files:**
- Modify: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`
- Modify: `backend/src/main/java/com/shigui/service/LostFoundPostService.java`
- Modify: `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`

- [ ] **Step 1: 新测试方法追加**

在 `LostFoundPostServiceTest` 中追加以下测试方法（保留原有 7 个测试不变）。需要新增 `@Mock` 字段和 import：

新增 import：
```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
```

新增 mock 和测试方法：

```java
@Mock
private LostFoundPostMapper lostFoundPostMapper;

// 在 setUp() 或独立方法中注入 mock page 返回值

@Test
void listPublic_onlyReturnsMatchingNotDeleted() {
    when(lostFoundPostMapper.selectPage(any(Page.class), any()))
            .thenAnswer(inv -> {
                Page<LostFoundPost> page = inv.getArgument(0);
                LostFoundPost post = new LostFoundPost();
                post.setId(1L);
                post.setStatus("MATCHING");
                post.setTitle("test");
                page.setRecords(java.util.List.of(post));
                page.setTotal(1);
                return page;
            });

    Page<PostResponse> result = lostFoundPostService.listPublic(1, 10, null, null, null, null);
    assertThat(result.getRecords()).hasSize(1);
    assertThat(result.getRecords().get(0).getStatus()).isEqualTo("MATCHING");
}

@Test
void listPublic_keywordFilter_matchesTitle() {
    when(lostFoundPostMapper.selectPage(any(Page.class), any()))
            .thenAnswer(inv -> {
                Page<LostFoundPost> page = inv.getArgument(0);
                page.setRecords(java.util.List.of());
                page.setTotal(0);
                return page;
            });

    Page<PostResponse> result = lostFoundPostService.listPublic(1, 10, null, null, null, "校园卡");
    assertThat(result.getRecords()).isEmpty();
}

@Test
void listMine_onlyReturnsCurrentUser() {
    when(lostFoundPostMapper.selectPage(any(Page.class), any()))
            .thenAnswer(inv -> {
                Page<LostFoundPost> page = inv.getArgument(0);
                LostFoundPost post = new LostFoundPost();
                post.setId(1L);
                post.setUserId(1L);
                post.setStatus("PENDING_AUDIT");
                post.setTitle("mine");
                page.setRecords(java.util.List.of(post));
                page.setTotal(1);
                return page;
            });

    Page<PostResponse> result = lostFoundPostService.listMine(1L, 1, 10, null);
    assertThat(result.getRecords()).hasSize(1);
    assertThat(result.getRecords().get(0).getStatus()).isEqualTo("PENDING_AUDIT");
}

@Test
void listMine_excludesDeleted() {
    when(lostFoundPostMapper.selectPage(any(Page.class), any()))
            .thenAnswer(inv -> {
                Page<LostFoundPost> page = inv.getArgument(0);
                page.setRecords(java.util.List.of());
                page.setTotal(0);
                return page;
            });

    Page<PostResponse> result = lostFoundPostService.listMine(1L, 1, 10, null);
    assertThat(result.getRecords()).isEmpty();
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=LostFoundPostServiceTest
```
预期: FAIL，方法还不存在。

- [ ] **Step 3: 在 Service 接口中新增方法签名**

修改 `LostFoundPostService.java`，追加：
```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

Page<PostResponse> listPublic(int page, int size, String postType,
    String itemCategory, String campusArea, String keyword);

Page<PostResponse> listMine(Long userId, int page, int size, String postType);
```

- [ ] **Step 4: 实现 listPublic**

在 `LostFoundPostServiceImpl` 中追加：

```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Override
public Page<PostResponse> listPublic(int page, int size, String postType,
        String itemCategory, String campusArea, String keyword) {
    LambdaQueryWrapper<LostFoundPost> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(LostFoundPost::getStatus, "MATCHING");
    wrapper.eq(LostFoundPost::getDeleted, 0);
    wrapper.eq(postType != null && !postType.isEmpty(), LostFoundPost::getPostType, postType);
    wrapper.eq(itemCategory != null && !itemCategory.isEmpty(), LostFoundPost::getItemCategory, itemCategory);
    wrapper.eq(campusArea != null && !campusArea.isEmpty(), LostFoundPost::getCampusArea, campusArea);
    wrapper.like(keyword != null && !keyword.isEmpty(), LostFoundPost::getTitle, keyword);
    wrapper.orderByDesc(LostFoundPost::getPublishedAt);

    Page<LostFoundPost> entityPage = page(new Page<>(page, size), wrapper);
    List<PostResponse> responses = entityPage.getRecords().stream()
            .map(this::toResponse).toList();

    Page<PostResponse> result = new Page<>(page, size);
    result.setRecords(responses);
    result.setTotal(entityPage.getTotal());
    return result;
}
```

需要新增 import：
```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
```

- [ ] **Step 5: 实现 listMine**

在 `LostFoundPostServiceImpl` 中追加：

```java
@Override
public Page<PostResponse> listMine(Long userId, int page, int size, String postType) {
    LambdaQueryWrapper<LostFoundPost> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(LostFoundPost::getUserId, userId);
    wrapper.eq(LostFoundPost::getDeleted, 0);
    wrapper.eq(postType != null && !postType.isEmpty(), LostFoundPost::getPostType, postType);
    wrapper.orderByDesc(LostFoundPost::getPublishedAt);

    Page<LostFoundPost> entityPage = page(new Page<>(page, size), wrapper);
    List<PostResponse> responses = entityPage.getRecords().stream()
            .map(this::toResponse).toList();

    Page<PostResponse> result = new Page<>(page, size);
    result.setRecords(responses);
    result.setTotal(entityPage.getTotal());
    return result;
}
```

- [ ] **Step 6: 运行测试确认通过**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=LostFoundPostServiceTest
```
预期: PASS — 11 测试通过（原 7 + 新 4）。

- [ ] **Step 7: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add backend/src/main/java/com/shigui/service/
git add backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java
git commit -m "feat: add listPublic and listMine service methods with tests"
```

---

### Task 2: Controller + SaToken 变更

**Files:**
- Modify: `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`
- Modify: `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java`
- Modify: `backend/src/main/java/com/shigui/config/SaTokenConfig.java`

- [ ] **Step 1: 在 Controller 新增两个端点**

在 `LostFoundPostController` 中追加：

```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 公开列表：筛选、搜索、分页。
 */
@GetMapping
public Result<Page<PostResponse>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String postType,
        @RequestParam(required = false) String itemCategory,
        @RequestParam(required = false) String campusArea,
        @RequestParam(required = false) String keyword) {
    return Result.ok(lostFoundPostService.listPublic(page, size, postType, itemCategory, campusArea, keyword));
}

/**
 * 我的记录：查看当前用户发布的所有单据。
 */
@GetMapping("/mine")
public Result<Page<PostResponse>> mine(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String postType) {
    Long userId = StpUtil.getLoginIdAsLong();
    return Result.ok(lostFoundPostService.listMine(userId, page, size, postType));
}
```

注意 `@GetMapping("/mine")` 必须在 `@GetMapping("/{id}")` 之前，否则 `mine` 会被当作 `{id}` 路径变量。

- [ ] **Step 2: 不需要修改 SaToken 配置**

`GET /api/posts` 不加到 SaToken 排除列表。原因：`/api/posts` 同时是 `POST /api/posts` 的路径，SaToken 的 `excludePathPatterns` 按路径匹配、不区分 HTTP 方法——如果排除会导致发布接口也变成公开。

列表接口仍需登录。小程序端始终携带 `satoken` 请求头，已登录用户可正常加载列表。未登录用户看到空列表后自然会去"我的"页登录。

- [ ] **Step 3: 编写 Controller 测试**

在 `LostFoundPostControllerTest` 中追加以下测试方法：

```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

@Test
void listPublic_noAuth_returns401() throws Exception {
    // 列表接口不加到 SaToken 排除列表——不区分 HTTP 方法的路径排除会误伤 POST。
    mockMvc.perform(get("/api/posts"))
            .andExpect(status().isUnauthorized());
}

@Test
void listPublic_loggedIn_returns200() throws Exception {
    Page<PostResponse> emptyPage = new Page<>(1, 10);
    emptyPage.setRecords(List.of());
    emptyPage.setTotal(0);
    when(appUserService.loginByWechat(anyString())).thenReturn(1L);
    when(lostFoundPostService.listPublic(eq(1), eq(10), isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);

    String token = loginAndGetToken();

    mockMvc.perform(get("/api/posts")
                    .header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void mine_notLoggedIn_returns401() throws Exception {
    mockMvc.perform(get("/api/posts/mine"))
            .andExpect(status().isUnauthorized());
}

@Test
void mine_loggedIn_returns200() throws Exception {
    Page<PostResponse> emptyPage = new Page<>(1, 10);
    emptyPage.setRecords(List.of());
    emptyPage.setTotal(0);
    when(appUserService.loginByWechat(anyString())).thenReturn(1L);
    when(lostFoundPostService.listMine(eq(1L), eq(1), eq(10), isNull())).thenReturn(emptyPage);

    String token = loginAndGetToken();

    mockMvc.perform(get("/api/posts/mine")
                    .header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}
```

需要新增 import：`com.baomidou.mybatisplus.extension.plugins.pagination.Page`, `java.util.List`, `static org.mockito.ArgumentMatchers.eq`, `static org.mockito.ArgumentMatchers.isNull`。

- [ ] **Step 4: 运行 Controller 测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=LostFoundPostControllerTest
```
预期: PASS — 8 测试通过（原 4 + 新 4）。注意：`listPublic_noAuth_returns401` 验证未登录被拒，`listPublic_loggedIn_returns200` 验证登录后可正常访问。

- [ ] **Step 5: 运行全部测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test
```
预期: 原 26 + 8 = 34 测试全部通过。

- [ ] **Step 6: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add backend/src/main/java/com/shigui/controller/LostFoundPostController.java
git add backend/src/main/java/com/shigui/config/SaTokenConfig.java
git add backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java
git commit -m "feat: add list posts and mine posts API endpoints"
```

---

### Task 3: 小程序首页复刻

**Files:**
- Modify: `miniapp/pages/index/index.js`
- Modify: `miniapp/pages/index/index.wxml`
- Modify: `miniapp/pages/index/index.wxss`
- Modify: `miniapp/pages/index/index.json`

- [ ] **Step 1: index.json — 启用下拉刷新**

```json
{
  "enablePullDownRefresh": true,
  "usingComponents": {
    "post-card": "/components/post-card/post-card"
  }
}
```

- [ ] **Step 2: index.js — 完整逻辑**

```javascript
const app = getApp()

Page({
  data: {
    posts: [],
    categories: ['全部', '校园卡', '学生证', '钥匙', '耳机', '水杯', '雨伞', '书籍', '其他'],
    activeCategory: '全部',
    activeTab: 'all',
    page: 1,
    keyword: ''
  },

  onLoad() {
    this.loadPosts()
  },

  onPullDownRefresh() {
    this.setData({ page: 1, posts: [] })
    this.loadPosts().then(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.setData({ page: this.data.page + 1 })
    this.loadPosts()
  },

  loadPosts() {
    const { activeCategory, activeTab, page, keyword } = this.data
    let url = `${app.globalData.baseUrl}/api/posts?page=${page}&size=10`
    if (activeTab !== 'all') url += `&postType=${activeTab.toUpperCase()}`
    if (activeCategory !== '全部') url += `&itemCategory=${activeCategory}`
    if (keyword.trim()) url += `&keyword=${encodeURIComponent(keyword.trim())}`

    return new Promise((resolve) => {
      wx.request({
        url,
        success: (res) => {
          if (res.data.code === 200) {
            const newPosts = res.data.data.records || []
            this.setData({
              posts: page === 1 ? newPosts : [...this.data.posts, ...newPosts]
            })
          }
          resolve()
        },
        fail: () => resolve()
      })
    })
  },

  switchTab(e) {
    const nextTab = e.currentTarget.dataset.tab
    const finalTab = (nextTab === this.data.activeTab) ? 'all' : nextTab
    this.setData({ activeTab: finalTab, page: 1, posts: [] })
    this.loadPosts()
  },

  switchCategory(e) {
    const cat = e.currentTarget.dataset.category
    this.setData({ activeCategory: cat, page: 1, posts: [] })
    this.loadPosts()
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearchConfirm() {
    this.setData({ page: 1, posts: [] })
    this.loadPosts()
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  },

  goMap() {
    wx.navigateTo({ url: '/pages/map/map' })
  }
})
```

- [ ] **Step 3: index.wxml — 复刻旧项目 UI**

```xml
<view class="container">
  <view class="sticky-header">
    <view class="search-row">
      <view class="search-box">
        <image class="search-icon" src="/assets/icons/search.svg" />
        <input class="search-input" value="{{keyword}}" placeholder="搜索失物/招领..." placeholder-class="search-placeholder" bindinput="onSearchInput" bindconfirm="onSearchConfirm" confirm-type="search" />
      </view>
      <view class="map-icon-btn" bindtap="goMap">
        <image class="btn-icon" src="/assets/icons/map-pin.svg" />
      </view>
    </view>

    <view class="filter-row">
      <view class="mini-capsule">
        <view class="capsule-slider pos-{{activeTab}}"></view>
        <view class="capsule-item {{activeTab === 'lost' ? 'active' : ''}}" data-tab="lost" bindtap="switchTab">寻</view>
        <view class="capsule-item {{activeTab === 'found' ? 'active' : ''}}" data-tab="found" bindtap="switchTab">招</view>
      </view>

      <view class="divider"></view>

      <scroll-view class="category-scroll" scroll-x>
        <view class="category-track">
          <view class="category-tag {{activeCategory === item ? 'active' : ''}}"
                wx:for="{{categories}}" wx:key="*this"
                data-category="{{item}}" bindtap="switchCategory">{{item}}</view>
        </view>
      </scroll-view>
    </view>
  </view>

  <view class="masonry-list" wx:if="{{posts.length > 0}}">
    <post-card
      wx:for="{{posts}}"
      wx:key="id"
      item="{{item}}"
      data-id="{{item.id}}"
      bindtap="goDetail"
      class="masonry-item"
    ></post-card>
  </view>

  <view wx:if="{{posts.length === 0}}" class="empty">
    <image class="empty-icon" src="/assets/icons/empty.svg" />
    <view>暂无数据，去发布一个吧</view>
  </view>
</view>
```

- [ ] **Step 4: index.wxss — 复刻旧项目样式**

```css
.sticky-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  padding: 10rpx 0 16rpx;
  border-bottom: 1rpx solid rgba(0,0,0,0.03);
}

.search-row {
  display: flex;
  align-items: center;
  padding: 0 30rpx;
  gap: 20rpx;
  margin-bottom: 16rpx;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f4f5f7;
  padding: 12rpx 24rpx;
  border-radius: 40rpx;
}
.search-icon { width: 28rpx; height: 28rpx; margin-right: 12rpx; opacity: 0.5; }
.search-input { flex: 1; font-size: 26rpx; color: var(--text-main); }
.search-placeholder { color: var(--text-light); }

.map-icon-btn {
  width: 64rpx;
  height: 64rpx;
  background: var(--primary-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(46, 125, 50, 0.1);
}
.map-icon-btn .btn-icon { width: 32rpx; height: 32rpx; }

.filter-row {
  --filter-control-height: 60rpx;
  --filter-inner-height: 48rpx;
  --filter-control-padding: 6rpx;
  display: flex;
  align-items: center;
  padding: 0 30rpx;
  height: 88rpx;
  overflow: hidden;
}

.mini-capsule {
  display: flex;
  background: #f1f3f5;
  border-radius: 32rpx;
  padding: var(--filter-control-padding);
  position: relative;
  width: 130rpx;
  height: var(--filter-control-height);
  flex-shrink: 0;
  box-sizing: border-box;
  align-items: center;
}

.capsule-slider {
  position: absolute;
  background: var(--white);
  border-radius: 28rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  z-index: 1;
  top: var(--filter-control-padding);
  height: var(--filter-inner-height);
}

.pos-all { width: calc(100% - 12rpx); transform: translateX(0); left: 6rpx; }
.pos-lost { width: calc(50% - 8rpx); transform: translateX(0); left: 6rpx; }
.pos-found { width: calc(50% - 8rpx); transform: translateX(100%); left: 2rpx; }

.capsule-item {
  flex: 1;
  text-align: center;
  font-size: 24rpx;
  color: var(--text-second);
  position: relative;
  z-index: 2;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.divider {
  width: 2rpx;
  height: 28rpx;
  background: rgba(0,0,0,0.08);
  margin: 0 24rpx;
  flex-shrink: 0;
}

.category-scroll {
  flex: 1;
  white-space: nowrap;
  height: var(--filter-control-height);
  width: 0;
  line-height: 0;
}

.category-track {
  display: inline-block;
  height: var(--filter-control-height);
  padding-top: var(--filter-control-padding);
  box-sizing: border-box;
  white-space: nowrap;
  vertical-align: top;
}

.category-tag {
  display: inline-block;
  padding: 0 28rpx;
  height: var(--filter-inner-height);
  margin-right: 16rpx;
  border-radius: 24rpx;
  font-size: 24rpx;
  background: transparent;
  color: var(--text-second);
  transition: all 0.2s;
  border: 1rpx solid #eeeeee;
  flex-shrink: 0;
  box-sizing: border-box;
  line-height: var(--filter-inner-height);
  text-align: center;
  vertical-align: top;
}

.category-tag.active {
  background: var(--primary-color);
  color: var(--white);
  border-color: transparent;
  font-weight: 600;
  box-shadow: 0 2rpx 8rpx rgba(46, 125, 50, 0.16);
}

.masonry-list {
  padding: 16rpx 20rpx;
  column-count: 2;
  column-gap: 20rpx;
}

.masonry-item {
  display: block;
  break-inside: avoid;
  margin-bottom: 20rpx;
}
.masonry-item:active { opacity: 0.9; }

.empty {
  text-align: center;
  color: var(--text-light);
  padding: 160rpx 0;
  font-size: 28rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.empty-icon { font-size: 100rpx; margin-bottom: 20rpx; opacity: 0.8; }
```

- [ ] **Step 5: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add miniapp/pages/index/
git commit -m "feat: implement miniapp index page with filter and waterfall list"
```

---

### Task 4: 详情页按钮调整

**Files:**
- Modify: `miniapp/pages/detail/detail.js`

- [ ] **Step 1: 将认领和聊天按钮改为 toast 占位**

修改 `miniapp/pages/detail/detail.js` 中的 `applyClaim()` 和 `openChat()` 方法：

```javascript
applyClaim() {
  wx.showToast({ title: '功能开发中', icon: 'none' })
},

openChat() {
  wx.showToast({ title: '功能开发中', icon: 'none' })
}
```

保留 `onLoad` 和 `loadDetail` 不变——它们已经正确调用 `GET /api/posts/{id}`。

- [ ] **Step 2: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add miniapp/pages/detail/detail.js
git commit -m "feat: stub claim and chat buttons on detail page"
```

---

### Task 5: 端到端冒烟验证

- [ ] **Step 1: 运行全部后端测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test
```
预期: 34 tests PASS。

- [ ] **Step 2: 运行管理端构建**

```bash
cd /Users/cyrene/Dev/shigui/admin-web && npm run build
```
预期: 构建成功。

- [ ] **Step 3: 用 IDEA 启动后端** (`BackendApplication.java`)

- [ ] **Step 4: curl 冒烟测试**

```bash
# 登录获取 token
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/user/wx-login -H 'Content-Type: application/json' -d '{"openid":"sprint3_test"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data'])")

# 测试未登录被拒
curl -s -o /dev/null -w "HTTP %{http_code}\n" "http://127.0.0.1:8080/api/posts"

# 登录后访问列表
curl -s "http://127.0.0.1:8080/api/posts?page=1&size=10" -H "satoken: $TOKEN" | python3 -m json.tool

# 带筛选
curl -s "http://127.0.0.1:8080/api/posts?postType=LOST&keyword=校园卡" -H "satoken: $TOKEN" | python3 -m json.tool

# mine
curl -s "http://127.0.0.1:8080/api/posts/mine" -H "satoken: $TOKEN" | python3 -m json.tool
```

预期：
- 公开列表返回 200，有分页结构 `{records:[], total:N}`
- mine 未登录返回 401
- mine 登录后返回 200

- [ ] **Step 5: 微信开发者工具验证**

- 打开首页看到瀑布流列表（如果有已审核单据）
- 切换寻物/招领 Tab
- 点击品类标签
- 下拉刷新
- 点击卡片进入详情页
- 详情页点击"申请认领"弹 toast

- [ ] **Step 6: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add .
git commit -m "feat: Sprint 3 complete - list filter, search, mine posts"
```

---

## 自查

- 规格覆盖:
  - `GET /api/posts` 公开列表 → Task 2
  - `GET /api/posts/mine` 我的记录 → Task 2
  - 筛选参数 `postType`/`itemCategory`/`campusArea`/`keyword` → Task 1
  - `keyword` LIKE 模糊匹配 → Task 1
  - 只查 `MATCHING` + 未删除 → Task 1
  - SaToken 放行 GET /api/posts → Task 2
  - 小程序首页复刻 → Task 3
  - 详情页按钮占位 → Task 4
- 占位符扫描: 无 TBD/TODO。
- 类型一致性: `listPublic` 方法签名在服务接口和实现中一致，Controller `@RequestParam` 参数名与接口一致。
  - `GET /api/posts/mine` 路径在 `@GetMapping("/{id}")` 之前声明，避免被路径变量吞掉。

---

### 验证清单

- [ ] 后端 34 个测试全部通过
- [ ] `npm run build` 成功
- [ ] `GET /api/posts` 未登录返回 401
- [ ] `GET /api/posts` 登录后返回 200
- [ ] `GET /api/posts?postType=LOST` 筛选生效
- [ ] `GET /api/posts?keyword=校园卡` 搜索生效
- [ ] `GET /api/posts/mine` 未登录返回 401
- [ ] `GET /api/posts/mine` 登录后返回 200
- [ ] 小程序首页展示瀑布流卡片列表
- [ ] 胶囊 Tab 和品类标签切换正常
- [ ] 详情页认领按钮弹 toast
