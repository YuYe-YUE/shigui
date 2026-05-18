// 匹配列表页：展示系统智能匹配的失物与招领结果
const app = getApp()
Page({
  data: { matches: [], page: 1 },
  // 页面加载时拉取匹配列表
  onLoad() { this.loadMatches() },
  // 触底时翻页追加数据
  onReachBottom() { this.setData({ page: this.data.page + 1 }); this.loadMatches() },
  // 请求当前用户的匹配记录，支持分页
  loadMatches() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/matches/mine?page=${this.data.page}&size=10`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          const records = res.data.data.records || []
          this.setData({ matches: this.data.page === 1 ? records : [...this.data.matches, ...records] })
        }
      },
      fail: () => wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },
  // 点击匹配项跳转帖子详情
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  }
})
