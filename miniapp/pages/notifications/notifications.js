const app = getApp()
Page({
  data: { notifications: [], page: 1 },
  onLoad() { this.loadNotifications() },
  onReachBottom() { this.setData({ page: this.data.page + 1 }); this.loadNotifications() },
  loadNotifications() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/notifications?page=${this.data.page}&size=10`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          const records = res.data.data.records || []
          this.setData({ notifications: this.data.page === 1 ? records : [...this.data.notifications, ...records] })
        }
      }
    })
  },
  goMatch(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/matches/matches?id=${id}` })
  }
})
