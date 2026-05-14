const app = getApp()
Page({
  data: { matches: [], page: 1 },
  onLoad() { this.loadMatches() },
  onReachBottom() { this.setData({ page: this.data.page + 1 }); this.loadMatches() },
  loadMatches() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/matches/mine?page=${this.data.page}&size=10`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          const records = res.data.data.records || []
          this.setData({ matches: this.data.page === 1 ? records : [...this.data.matches, ...records] })
        }
      }
    })
  },
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  }
})
