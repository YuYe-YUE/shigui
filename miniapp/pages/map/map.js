// 地图页：展示校园内招领物品的图钉位置，点击可查看详情
const app = getApp()
const EAST_CAMPUS_CENTER = {
  latitude: 23.06,
  longitude: 113.39
}

Page({
  data: {
    latitude: EAST_CAMPUS_CENTER.latitude,
    longitude: EAST_CAMPUS_CENTER.longitude,
    scale: 15,
    markers: [],
    postsById: {},
    selectedPost: null,
    loading: false
  },

  // 页面加载时获取地图点位数据
  onLoad() {
    this.loadMapPoints()
  },

  // 请求后端地图点位接口，构建 marker 数组
  loadMapPoints() {
    this.setData({ loading: true })
    wx.request({
      url: `${app.globalData.baseUrl}/api/posts/map`,
      success: (res) => {
        if (res.data.code !== 200) {
          wx.showToast({ title: res.data.message || '地图点位加载失败', icon: 'none' })
          this.setData({
            markers: [],
            postsById: {},
            selectedPost: null
          })
          return
        }
        const posts = Array.isArray(res.data.data) ? res.data.data : []
        const postsById = {}
        const markers = posts.map((post) => {
          const normalizedPost = {
            ...post,
            displayEventTime: this.formatEventTime(post.eventTime)
          }
          postsById[post.id] = normalizedPost
          return {
            id: normalizedPost.id,
            latitude: normalizedPost.latitude,
            longitude: normalizedPost.longitude,
            width: 30,
            height: 30,
            callout: {
              content: [
                normalizedPost.itemName || '招领物品',
                `${normalizedPost.itemCategory || '其他'} · ${normalizedPost.campusArea || '未知校区'}`,
                normalizedPost.locationName || '未知地点',
                normalizedPost.displayEventTime
              ].join('\n'),
              fontSize: 12,
              padding: 8,
              borderRadius: 10,
              display: 'BYCLICK'
            },
            label: {
              content: `${this.getCategoryLabel(normalizedPost.itemCategory)} ${normalizedPost.itemName || '招领物品'}`,
              fontSize: 11,
              color: '#00573D',
              bgColor: '#FFFFFF',
              borderRadius: 14,
              padding: 6,
              anchorX: -12,
              anchorY: -44
            }
          }
        })
        this.setData({
          markers,
          postsById,
          selectedPost: null
        })
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' })
        this.setData({
          markers: [],
          postsById: {},
          selectedPost: null
        })
      },
      complete: () => {
        this.setData({ loading: false })
      }
    })
  },

  // 根据物品分类返回对应 emoji 图标
  getCategoryLabel(category) {
    const map = {
      '证件': '📇',
      '钥匙': '🔑',
      '数码': '🎧',
      '书籍': '📖',
      '衣物': '🧥',
      '雨伞': '🌂',
      '其他': '📦'
    }
    return map[category] || '📦'
  },

  // 将事件时间格式化为可读字符串
  formatEventTime(eventTime) {
    if (!eventTime) {
      return '时间未知'
    }
    return eventTime.replace('T', ' ')
  },

  // 点击图钉时选中对应帖子，弹出气泡
  onMarkerTap(e) {
    const id = e.detail.markerId
    this.setData({ selectedPost: this.data.postsById[id] || null })
  },

  // 从弹窗跳转到帖子详情页
  goDetail() {
    if (this.data.selectedPost) {
      wx.navigateTo({ url: `/pages/detail/detail?id=${this.data.selectedPost.id}` })
    }
  },

  // 关闭弹窗
  hidePopup() {
    this.setData({ selectedPost: null })
  }
})
