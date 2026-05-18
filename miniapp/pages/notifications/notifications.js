// 通知列表页：展示系统推送的通知消息
const app = getApp()
Page({
  data: { notifications: [], page: 1 },
  // 加载页面时请求第一页通知
  onLoad() { this.loadNotifications() },
  // 触底翻页
  onReachBottom() { this.setData({ page: this.data.page + 1 }); this.loadNotifications() },
  // 请求通知列表，支持分页
  loadNotifications() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/notifications?page=${this.data.page}&size=10`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          const records = res.data.data.records || []
          this.setData({ notifications: this.data.page === 1 ? records : [...this.data.notifications, ...records] })
        }
      },
      fail: () => wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },
  // 点击通知跳转到匹配详情
  goMatch(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/matches/matches?id=${id}` })
  }
})
