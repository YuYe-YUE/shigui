# Sprint 7: 招领地图图钉 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现“发布招领单时可标注捡到位置，地图页只展示有坐标的招领点位”的 S7 闭环。

**Architecture:** 后端新增专用 `MapPostResponse`，由 `LostFoundPostService.listMapPosts()` 查询并脱敏映射，`GET /api/posts/map` 公开返回招领地图点。小程序发布表单用 `wx.chooseLocation()` 作为可选增强，不阻塞招领发布；地图页只消费地图 DTO 字段，展示招领语义的 marker 与底部卡片。

**Tech Stack:** Spring Boot 3.5.14, MyBatis-Plus 3.5.16, Sa-Token 1.45.0, Java 21, Maven, 原生微信小程序 `wx.chooseLocation` 和 `map` 组件。

---

## 文件结构

- Create: `backend/src/main/java/com/shigui/dto/MapPostResponse.java`  
  职责：公开地图点位响应 DTO，只包含地图展示必需字段。
- Modify: `backend/src/main/java/com/shigui/service/LostFoundPostService.java`  
  职责：增加 `listMapPosts()` Service 契约。
- Modify: `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`  
  职责：查询 `FOUND + MATCHING + deleted=0 + 有坐标` 的招领单，并映射到 `MapPostResponse`。
- Modify: `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`  
  职责：把 `/api/posts/map` 从空数组占位改成真实 Service 调用。
- Modify: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`  
  职责：覆盖地图点位查询条件和脱敏字段。
- Modify: `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java`  
  职责：覆盖公开地图接口 HTTP 契约。
- Modify: `miniapp/pages/publish-form/publish-form.js`  
  职责：招领单可选调用 `wx.chooseLocation()`，提交坐标。
- Modify: `miniapp/pages/publish-form/publish-form.wxml`  
  职责：增加“捡到位置”选点 UI。
- Modify: `miniapp/pages/publish-form/publish-form.wxss`  
  职责：增加选点 UI 样式。
- Modify: `miniapp/pages/map/map.js`  
  职责：消费 `MapPostResponse`，只展示招领点位。
- Modify: `miniapp/pages/map/map.wxml`  
  职责：展示空态和招领底部卡片。
- Modify: `miniapp/pages/map/map.wxss`  
  职责：补齐空态和底部卡片样式。
- Modify: `miniapp/pages/map/map.json`  
  职责：导航标题改为“地图找招领”。

---

### Task 1: 后端地图点位 DTO 与 Service

**Files:**
- Create: `backend/src/main/java/com/shigui/dto/MapPostResponse.java`
- Modify: `backend/src/main/java/com/shigui/service/LostFoundPostService.java`
- Modify: `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`
- Test: `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java`

- [ ] **Step 1: 写会失败的 Service 测试**

在 `backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java` 增加 import：

```java
import com.shigui.dto.MapPostResponse;

import java.util.List;
```

在测试类中增加两个测试：

```java
@Test
void listMapPosts_onlyReturnsMatchingFoundPostsWithCoordinates() {
    when(lostFoundPostMapper.selectList(any())).thenReturn(java.util.List.of(mapFoundPost()));

    List<MapPostResponse> result = lostFoundPostService.listMapPosts();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(88L);
    assertThat(result.get(0).getItemName()).isEqualTo("校园卡");
    assertThat(result.get(0).getItemCategory()).isEqualTo("证件");
    assertThat(result.get(0).getCampusArea()).isEqualTo("南校园");
    assertThat(result.get(0).getLocationName()).isEqualTo("逸夫楼门口");
    assertThat(result.get(0).getLongitude()).isEqualTo(113.2931234);
    assertThat(result.get(0).getLatitude()).isEqualTo(23.0961234);
    assertThat(result.get(0).getEventTime()).isEqualTo(LocalDateTime.of(2026, 5, 13, 9, 30));
}

@Test
void listMapPosts_responseDoesNotExposePrivateFields() throws Exception {
    when(lostFoundPostMapper.selectList(any())).thenReturn(java.util.List.of(mapFoundPost()));

    List<MapPostResponse> result = lostFoundPostService.listMapPosts();

    assertThat(result).hasSize(1);
    assertThat(MapPostResponse.class.getDeclaredField("id")).isNotNull();
    assertThatThrownBy(() -> MapPostResponse.class.getDeclaredField("privateFeature"))
            .isInstanceOf(NoSuchFieldException.class);
    assertThatThrownBy(() -> MapPostResponse.class.getDeclaredField("storageLocation"))
            .isInstanceOf(NoSuchFieldException.class);
    assertThatThrownBy(() -> MapPostResponse.class.getDeclaredField("userId"))
            .isInstanceOf(NoSuchFieldException.class);
    assertThatThrownBy(() -> MapPostResponse.class.getDeclaredField("description"))
            .isInstanceOf(NoSuchFieldException.class);
    assertThatThrownBy(() -> MapPostResponse.class.getDeclaredField("title"))
            .isInstanceOf(NoSuchFieldException.class);
    assertThatThrownBy(() -> MapPostResponse.class.getDeclaredField("status"))
            .isInstanceOf(NoSuchFieldException.class);
}
```

在测试类底部增加 helper：

```java
private LostFoundPost mapFoundPost() {
    LostFoundPost post = new LostFoundPost();
    post.setId(88L);
    post.setUserId(3L);
    post.setPostType("FOUND");
    post.setTitle("捡到校园卡");
    post.setItemName("校园卡");
    post.setItemCategory("证件");
    post.setDescription("绿色卡套");
    post.setPrivateFeature("卡号后四位 1234");
    post.setCampusArea("南校园");
    post.setLocationName("逸夫楼门口");
    post.setLongitude(113.2931234);
    post.setLatitude(23.0961234);
    post.setStorageLocation("保卫处前台");
    post.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));
    post.setStatus("MATCHING");
    post.setDeleted(0);
    return post;
}
```

- [ ] **Step 2: 运行测试，确认红灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest
```

预期：FAIL，编译错误包含 `cannot find symbol class MapPostResponse` 或 `cannot find symbol method listMapPosts()`。

- [ ] **Step 3: 创建地图响应 DTO**

创建 `backend/src/main/java/com/shigui/dto/MapPostResponse.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MapPostResponse {
    private Long id;
    private String itemName;
    private String itemCategory;
    private String campusArea;
    private String locationName;
    private Double longitude;
    private Double latitude;
    private LocalDateTime eventTime;
}
```

- [ ] **Step 4: 扩展 Service 接口**

修改 `backend/src/main/java/com/shigui/service/LostFoundPostService.java`：

```java
package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.MapPostResponse;
import com.shigui.dto.PostResponse;
import com.shigui.entity.LostFoundPost;

import java.util.List;

public interface LostFoundPostService extends IService<LostFoundPost> {
    PostResponse publish(Long userId, CreatePostRequest request);
    PostResponse getDetail(Long postId, Long currentUserId);

    Page<PostResponse> listPublic(int page, int size, String postType,
            String itemCategory, String campusArea, String keyword);

    Page<PostResponse> listMine(Long userId, int page, int size, String postType);

    List<MapPostResponse> listMapPosts();
}
```

- [ ] **Step 5: 实现地图查询与脱敏映射**

修改 `backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java`。

在 import 区增加：

```java
import com.shigui.dto.MapPostResponse;
```

在 `listMine` 方法后增加：

```java
@Override
public List<MapPostResponse> listMapPosts() {
    LambdaQueryWrapper<LostFoundPost> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(LostFoundPost::getPostType, "FOUND");
    wrapper.eq(LostFoundPost::getStatus, "MATCHING");
    wrapper.eq(LostFoundPost::getDeleted, 0);
    wrapper.isNotNull(LostFoundPost::getLongitude);
    wrapper.isNotNull(LostFoundPost::getLatitude);
    wrapper.orderByDesc(LostFoundPost::getPublishedAt);

    return list(wrapper).stream().map(this::toMapResponse).toList();
}

private MapPostResponse toMapResponse(LostFoundPost post) {
    MapPostResponse response = new MapPostResponse();
    response.setId(post.getId());
    response.setItemName(post.getItemName());
    response.setItemCategory(post.getItemCategory());
    response.setCampusArea(post.getCampusArea());
    response.setLocationName(post.getLocationName());
    response.setLongitude(post.getLongitude());
    response.setLatitude(post.getLatitude());
    response.setEventTime(post.getEventTime());
    return response;
}
```

- [ ] **Step 6: 运行 Service 测试，确认绿灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest
```

预期：PASS，`LostFoundPostServiceTest` 全部通过。

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/shigui/dto/MapPostResponse.java \
        backend/src/main/java/com/shigui/service/LostFoundPostService.java \
        backend/src/main/java/com/shigui/service/impl/LostFoundPostServiceImpl.java \
        backend/src/test/java/com/shigui/service/LostFoundPostServiceTest.java
git commit -m "feat: add found post map point service"
```

---

### Task 2: 后端地图 Controller 契约

**Files:**
- Modify: `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`
- Test: `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java`

- [ ] **Step 1: 写会失败的 Controller 测试**

在 `backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java` 增加 import：

```java
import com.shigui.dto.MapPostResponse;
```

在测试类中增加测试：

```java
@Test
void mapPoints_noAuth_returnsFoundMarkers() throws Exception {
    MapPostResponse marker = new MapPostResponse();
    marker.setId(88L);
    marker.setItemName("校园卡");
    marker.setItemCategory("证件");
    marker.setCampusArea("南校园");
    marker.setLocationName("逸夫楼门口");
    marker.setLongitude(113.2931234);
    marker.setLatitude(23.0961234);
    marker.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));
    when(lostFoundPostService.listMapPosts()).thenReturn(List.of(marker));

    mockMvc.perform(get("/api/posts/map"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].id").value(88))
            .andExpect(jsonPath("$.data[0].itemName").value("校园卡"))
            .andExpect(jsonPath("$.data[0].itemCategory").value("证件"))
            .andExpect(jsonPath("$.data[0].locationName").value("逸夫楼门口"))
            .andExpect(jsonPath("$.data[0].longitude").value(113.2931234))
            .andExpect(jsonPath("$.data[0].latitude").value(23.0961234))
            .andExpect(jsonPath("$.data[0].privateFeature").doesNotExist())
            .andExpect(jsonPath("$.data[0].storageLocation").doesNotExist())
            .andExpect(jsonPath("$.data[0].userId").doesNotExist())
            .andExpect(jsonPath("$.data[0].description").doesNotExist())
            .andExpect(jsonPath("$.data[0].title").doesNotExist())
            .andExpect(jsonPath("$.data[0].status").doesNotExist());
}
```

- [ ] **Step 2: 运行测试，确认红灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostControllerTest#mapPoints_noAuth_returnsFoundMarkers
```

预期：FAIL，接口仍返回空数组，失败信息包含 `No value at JSON path "$.data[0].id"`。

- [ ] **Step 3: 实现 Controller 调用 Service**

修改 `backend/src/main/java/com/shigui/controller/LostFoundPostController.java`。

增加 import：

```java
import com.shigui.dto.MapPostResponse;

import java.util.List;
```

把 `mapPoints()` 替换为：

```java
/**
 * 地图点位（公开）。只返回有坐标的招领单，且不返回私密特征、暂存地点和用户信息。
 */
@GetMapping("/map")
public Result<List<MapPostResponse>> mapPoints() {
    return Result.ok(lostFoundPostService.listMapPosts());
}
```

- [ ] **Step 4: 运行 Controller 测试，确认绿灯**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostControllerTest#mapPoints_noAuth_returnsFoundMarkers
```

预期：PASS。

- [ ] **Step 5: 运行后端相关测试**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=LostFoundPostServiceTest,LostFoundPostControllerTest
```

预期：PASS，两个测试类全部通过。

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/shigui/controller/LostFoundPostController.java \
        backend/src/test/java/com/shigui/controller/LostFoundPostControllerTest.java
git commit -m "feat: expose found post map points API"
```

---

### Task 3: 小程序发布招领单可选地图标注

**Files:**
- Modify: `miniapp/pages/publish-form/publish-form.js`
- Modify: `miniapp/pages/publish-form/publish-form.wxml`
- Modify: `miniapp/pages/publish-form/publish-form.wxss`

- [ ] **Step 1: 扩展表单数据结构**

修改 `miniapp/pages/publish-form/publish-form.js` 的 `form` 初始值：

```js
form: {
  itemName: '',
  itemCategory: '',
  description: '',
  privateFeature: '',
  campusArea: '',
  locationName: '',
  longitude: null,
  latitude: null,
  eventTime: '',
  storageLocation: ''
},
```

- [ ] **Step 2: 增加选择地图位置方法**

在 `selectCategory(e) { ... }` 后增加：

```js
chooseFoundLocation() {
  wx.chooseLocation({
    success: (res) => {
      const locationName = res.name || res.address || this.data.form.locationName
      this.setData({
        'form.locationName': locationName,
        'form.longitude': res.longitude,
        'form.latitude': res.latitude
      })
    },
    fail: () => {
      wx.showToast({ title: '未标注地图位置，单据不会显示在地图中', icon: 'none' })
    }
  })
},

clearFoundLocation() {
  this.setData({
    'form.longitude': null,
    'form.latitude': null
  })
},
```

- [ ] **Step 3: 提交 payload 带坐标**

把 `submit()` 中的 `payload` 替换为：

```js
const payload = {
  postType: this.data.postType,
  title: this.data.form.itemName,
  itemName: this.data.form.itemName,
  itemCategory: this.data.form.itemCategory,
  description: this.data.form.description,
  privateFeature: this.data.form.privateFeature,
  campusArea: this.data.form.campusArea,
  locationName: this.data.form.locationName,
  longitude: this.data.postType === 'FOUND' ? this.data.form.longitude : null,
  latitude: this.data.postType === 'FOUND' ? this.data.form.latitude : null,
  storageLocation: this.data.postType === 'FOUND' ? this.data.form.storageLocation : '',
  eventTime: this.data.form.eventTime ? `${this.data.form.eventTime}T00:00:00` : ''
}
```

- [ ] **Step 4: 招领单无坐标时给轻提示**

在 `const error = this.validate()` 之后、`this.setData({ submitting: true })` 之前增加：

```js
if (this.data.postType === 'FOUND' && (!this.data.form.longitude || !this.data.form.latitude)) {
  wx.showToast({ title: '未标注地图位置，单据不会显示在地图中', icon: 'none' })
}
```

此提示不 `return`，发布继续执行。

- [ ] **Step 5: 增加 WXML 选点 UI**

在 `miniapp/pages/publish-form/publish-form.wxml` 的“具体地点”表单块后增加：

```xml
<view class="form-group" wx:if="{{postType === 'FOUND'}}">
  <view class="form-label">捡到位置</view>
  <view class="location-panel">
    <view class="location-summary" wx:if="{{form.longitude && form.latitude}}">
      <view class="location-name">{{form.locationName || '已标注地图位置'}}</view>
      <view class="location-coords">{{form.latitude}}，{{form.longitude}}</view>
    </view>
    <view class="location-summary muted" wx:else>
      标注后，这条招领单会显示在地图中
    </view>
    <view class="location-actions">
      <button class="location-btn" size="mini" bindtap="chooseFoundLocation">在地图上标注</button>
      <button
        wx:if="{{form.longitude && form.latitude}}"
        class="location-clear"
        size="mini"
        bindtap="clearFoundLocation"
      >
        清除
      </button>
    </view>
  </view>
</view>
```

- [ ] **Step 6: 增加 WXSS 样式**

在 `miniapp/pages/publish-form/publish-form.wxss` 末尾增加：

```css
.location-panel {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
}

.location-summary {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 10px;
}

.location-summary.muted {
  color: #777;
}

.location-name {
  font-weight: 600;
}

.location-coords {
  color: #777;
  font-size: 12px;
  margin-top: 2px;
}

.location-actions {
  display: flex;
  gap: 8px;
}

.location-btn {
  background: #00573D;
  color: #fff;
  border-radius: 16px;
  margin: 0;
}

.location-clear {
  background: #f4f4f4;
  color: #555;
  border-radius: 16px;
  margin: 0;
}
```

- [ ] **Step 7: 手动验证发布 payload**

在微信开发者工具打开 `miniapp/`，执行：

1. 进入发布入口，选择“发布招领”。
2. 填写必填字段。
3. 点击 `在地图上标注` 并选择位置。
4. 提交。

预期：Network 面板里 `POST /api/posts` 请求体包含：

```json
{
  "postType": "FOUND",
  "locationName": "选中的地点名",
  "longitude": 113.2931234,
  "latitude": 23.0961234
}
```

再次验证取消选点：

1. 进入发布招领表单。
2. 不点击地图标注，直接填写文本地点并提交。

预期：显示 toast `未标注地图位置，单据不会显示在地图中`，请求仍发送，`longitude` 和 `latitude` 为 `null`。

- [ ] **Step 8: Commit**

```bash
git add miniapp/pages/publish-form/publish-form.js \
        miniapp/pages/publish-form/publish-form.wxml \
        miniapp/pages/publish-form/publish-form.wxss
git commit -m "feat: allow optional found location pin when publishing"
```

---

### Task 4: 小程序地图页展示招领点位

**Files:**
- Modify: `miniapp/pages/map/map.js`
- Modify: `miniapp/pages/map/map.wxml`
- Modify: `miniapp/pages/map/map.wxss`
- Modify: `miniapp/pages/map/map.json`

- [ ] **Step 1: 改造地图页 JS 数据流**

将 `miniapp/pages/map/map.js` 替换为：

```js
const app = getApp()

Page({
  data: {
    latitude: 23.06,
    longitude: 113.39,
    scale: 15,
    markers: [],
    postsById: {},
    selectedPost: null,
    loading: false
  },

  onLoad() {
    this.loadMapPoints()
  },

  loadMapPoints() {
    this.setData({ loading: true })
    wx.request({
      url: `${app.globalData.baseUrl}/api/posts/map`,
      success: (res) => {
        if (res.data.code !== 200) {
          wx.showToast({ title: res.data.message || '地图点位加载失败', icon: 'none' })
          return
        }
        const posts = res.data.data || []
        const postsById = {}
        const markers = posts.map((post) => {
          postsById[post.id] = post
          return {
            id: post.id,
            latitude: post.latitude,
            longitude: post.longitude,
            width: 30,
            height: 30,
            callout: {
              content: `${post.itemName || '招领物品'}\n${post.itemCategory || '其他'} · ${post.locationName || '未知地点'}`,
              fontSize: 13,
              padding: 8,
              display: 'BYCLICK'
            },
            label: {
              content: this.getCategoryLabel(post.itemCategory),
              fontSize: 12,
              anchorX: 0,
              anchorY: -35
            }
          }
        })
        const center = posts[0] || {}
        this.setData({
          markers,
          postsById,
          latitude: center.latitude || this.data.latitude,
          longitude: center.longitude || this.data.longitude
        })
      },
      fail: () => wx.showToast({ title: '网络错误', icon: 'none' }),
      complete: () => this.setData({ loading: false })
    })
  },

  getCategoryLabel(category) {
    const map = { '校园卡': '🎓', '学生证': '📇', '钥匙': '🔑', '耳机': '🎧', '水杯': '☕', '雨伞': '🌂', '书籍': '📖' }
    return map[category] || '📦'
  },

  onMarkerTap(e) {
    const id = e.detail.markerId
    this.setData({ selectedPost: this.data.postsById[id] || null })
  },

  goDetail() {
    if (this.data.selectedPost) {
      wx.navigateTo({ url: `/pages/detail/detail?id=${this.data.selectedPost.id}` })
    }
  },

  hidePopup() {
    this.setData({ selectedPost: null })
  }
})
```

- [ ] **Step 2: 改造地图页 WXML**

将 `miniapp/pages/map/map.wxml` 替换为：

```xml
<map
  class="full-map"
  latitude="{{latitude}}"
  longitude="{{longitude}}"
  scale="{{scale}}"
  markers="{{markers}}"
  bindmarkertap="onMarkerTap"
/>

<view class="empty-map" wx:if="{{!loading && markers.length === 0}}">
  暂无已标注位置的招领单
</view>

<view class="popup-mask" wx:if="{{selectedPost}}" catchtap="hidePopup"></view>

<view class="popup" wx:if="{{selectedPost}}">
  <view class="popup-header">
    <text class="popup-title">{{selectedPost.itemName || '招领物品'}}</text>
    <text class="popup-type type-found">招领</text>
  </view>
  <view class="popup-meta">{{selectedPost.itemCategory || '其他'}} · {{selectedPost.campusArea || '未知校区'}}</view>
  <view class="popup-location">{{selectedPost.locationName || '未知地点'}}</view>
  <view class="popup-time">{{selectedPost.eventTime || ''}}</view>
  <button class="popup-action" bindtap="goDetail">查看详情</button>
</view>
```

- [ ] **Step 3: 改造地图页 WXSS**

将 `miniapp/pages/map/map.wxss` 替换为：

```css
.full-map {
  width: 100%;
  height: 100vh;
}

.empty-map {
  position: fixed;
  top: 40rpx;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.94);
  color: #555;
  font-size: 26rpx;
  padding: 18rpx 28rpx;
  border-radius: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);
  z-index: 900;
}

.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
}

.popup {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 40rpx 30rpx;
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.08);
  z-index: 1000;
  animation: slideUp 0.25s ease-out;
}

.popup::before {
  content: '';
  position: absolute;
  top: 16rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 80rpx;
  height: 8rpx;
  background: #e0e0e0;
  border-radius: 4rpx;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.popup-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #222;
  flex: 1;
  margin-right: 20rpx;
}

.popup-type {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-weight: 600;
}

.type-found {
  background: #e9f7ef;
  color: #00573D;
}

.popup-meta,
.popup-location,
.popup-time {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 12rpx;
  line-height: 1.5;
}

.popup-location {
  color: #333;
  font-weight: 600;
}

.popup-action {
  background: #00573D;
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 600;
  margin-top: 24rpx;
}
```

- [ ] **Step 4: 修改地图页标题**

将 `miniapp/pages/map/map.json` 替换为：

```json
{
  "navigationBarTitleText": "地图找招领"
}
```

- [ ] **Step 5: 手动验证地图页**

在微信开发者工具打开 `miniapp/`，确保后端 `/api/posts/map` 有至少一条带坐标响应：

```json
{
  "id": 88,
  "itemName": "校园卡",
  "itemCategory": "证件",
  "campusArea": "南校园",
  "locationName": "逸夫楼门口",
  "longitude": 113.2931234,
  "latitude": 23.0961234,
  "eventTime": "2026-05-13T09:30:00"
}
```

验证步骤：

1. 从首页点击地图按钮进入地图页。
2. 地图出现 marker。
3. 点击 marker。
4. 底部卡片显示物品名称、类别、地点、时间。
5. 点击 `查看详情` 跳转到 `pages/detail/detail?id=88`。

空态验证：

1. 让 `/api/posts/map` 返回 `[]`。
2. 打开地图页。

预期：页面显示 `暂无已标注位置的招领单`。

- [ ] **Step 6: Commit**

```bash
git add miniapp/pages/map/map.js \
        miniapp/pages/map/map.wxml \
        miniapp/pages/map/map.wxss \
        miniapp/pages/map/map.json
git commit -m "feat: show found post map pins in miniapp"
```

---

### Task 5: 全量验证与收口

**Files:**
- Verify only.

- [ ] **Step 1: 后端全量测试**

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test
```

预期：BUILD SUCCESS。真实 AI API 测试在未配置环境变量时允许 `Skipped: 2`。

- [ ] **Step 2: 管理端构建**

```bash
cd /Users/cyrene/Dev/shigui/admin-web
npm run build
```

预期：构建成功。Vite 若提示 chunk size warning，不阻塞 S7。

- [ ] **Step 3: 小程序手动验收清单**

在微信开发者工具执行：

- 发布招领单，点击 `在地图上标注`，提交请求带 `longitude/latitude/locationName`。
- 发布招领单，不标注地图位置，仍能提交，页面提示不会显示在地图中。
- 发布寻物单，不显示地图标注模块。
- 地图页只展示 `/api/posts/map` 返回的招领 marker。
- marker 底部卡片不显示私密特征、暂存地点、用户 ID、描述全文。
- 点击底部卡片 `查看详情` 能进入详情页。

- [ ] **Step 4: 检查工作区只包含 S7 相关变更**

```bash
cd /Users/cyrene/Dev/shigui
git status --short
```

预期：准备提交的文件只包含本计划列出的 S7 文件。若工作区存在 S6 或本地配置变更，不要把它们混进 S7 commit。

- [ ] **Step 5: 最终 Commit**

如果 Task 1-4 已经分别 commit，本 Task 不需要额外提交。若 Step 3 的手动验收修了小问题，单独提交：

```bash
git add <S7相关文件>
git commit -m "fix: polish sprint 7 map pin flow"
```

---

## 自检清单

- S7 只展示 `FOUND + MATCHING + deleted=0 + 有经纬度` 的点位。
- `/api/posts/map` 返回 `MapPostResponse`，不返回实体和管理端 DTO。
- 地图接口不暴露 `privateFeature`、`storageLocation`、`userId`、`description`、`title`、`status`。
- 发布招领单可以标注地图位置，但不强制。
- 没有坐标的招领单仍可提交，只是不进入地图。
- 寻物单不显示地图标注入口，不进入地图页。
- 每个后端行为先写红灯测试，再实现，再运行绿灯测试。
- 每个 Task 结束一个 commit，避免混入 S6 修复和 `application-local.properties`。
