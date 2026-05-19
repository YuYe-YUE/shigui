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
          this.setData({ markers: [] })
          return
        }
        const posts = Array.isArray(res.data.data) ? res.data.data : []
        const markers = posts.map((post) => {
          return {
            id: post.id,
            latitude: post.latitude,
            longitude: post.longitude,
            width: 30,
            height: 30,
            label: {
              content: `${this.getCategoryLabel(post.itemCategory)} ${post.itemName || '招领物品'}`,
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
        this.setData({ markers })
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' })
        this.setData({ markers: [] })
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
      '校园卡': '📇',
      '学生证': '📇',
      '钥匙': '🔑',
      '耳机': '🎧',
      '数码': '🎧',
      '水杯': '🥤',
      '书籍': '📖',
      '衣物': '🧥',
      '雨伞': '🌂',
      '其他': '📦'
    }
    return map[category] || '📦'
  },

  // 点击图钉时直接跳转详情页
  onMarkerTap(e) {
    const id = e.detail.markerId
    if (id) {
      wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
    }
  }
})
