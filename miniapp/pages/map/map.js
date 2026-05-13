/**
 * 地图页 - 展示所有失物招领帖子的地理位置标记
 * 以地图标注形式展示每一条帖子的发生地点，点击标记可查看简要信息并跳转详情
 */
const app = getApp()

Page({
  data: {
    latitude: 23.06,
    longitude: 113.39,
    scale: 15,
    markers: [],
    selectedPost: null
  },

  /**
   * 页面加载时获取地图标记数据
   */
  onLoad() {
    this.loadMapPoints()
  },

  /**
   * 请求所有帖子的地理位置信息并生成地图标记点
   * 每个标记点用emoji图标显示物品品类
   */
  loadMapPoints() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/posts/map`,
      success: (res) => {
        if (res.data.code === 200) {
          const markers = res.data.data.map(p => ({
            id: p.id,
            latitude: p.latitude,
            longitude: p.longitude,
            title: p.title,
            width: 30,
            height: 30,
            callout: {
              content: `${p.title}\n${p.itemCategory} · ${p.locationName}`,
              fontSize: 13,
              padding: 8,
              display: 'BYCLICK'
            },
            // 用 label 显示品类文字
            label: {
              content: this.getCategoryLabel(p.itemCategory),
              fontSize: 12,
              anchorX: 0,
              anchorY: -35
            }
          }))
          this.setData({ markers })
        }
      }
    })
  },

  /**
   * 根据物品品类返回对应的emoji标签图标
   */
  getCategoryLabel(category) {
    const map = { '校园卡': '🎓', '学生证': '📇', '钥匙': '🔑', '耳机': '🎧', '水杯': '☕', '雨伞': '🌂', '书籍': '📖' }
    return map[category] || '📦'
  },

  /**
   * 地图标记点击事件 - 显示该帖子的简要信息弹窗
   */
  onMarkerTap(e) {
    const id = e.detail.markerId
    const post = this.data.markers.find(m => m.id === id)
    this.setData({ selectedPost: post || null })
  },

  /**
   * 从信息弹窗跳转到帖子详情页
   */
  goDetail() {
    if (this.data.selectedPost) {
      wx.navigateTo({ url: `/pages/detail/detail?id=${this.data.selectedPost.id}` })
    }
  },

  /**
   * 关闭帖子信息弹出层
   */
  hidePopup() {
    this.setData({ selectedPost: null })
  }
})
