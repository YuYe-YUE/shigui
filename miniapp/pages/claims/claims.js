// 认领记录页：展示用户发起的认领申请及其处理状态
const app = getApp()
Page({
  data: { claims: [], page: 1 },
  // 每次页面显示时重新加载认领列表
  onShow() { this.setData({ page: 1 }); this.loadClaims() },
  // 触底翻页
  onReachBottom() { this.setData({ page: this.data.page + 1 }); this.loadClaims() },
  // 请求当前用户的认领记录，支持分页
  loadClaims() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/claims/mine?page=${this.data.page}&size=10`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          const records = res.data.data.records || []
          this.setData({ claims: this.data.page === 1 ? records : [...this.data.claims, ...records] })
        }
      },
      fail: () => wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },
  // 确认收到物品
  confirmReceive(e) {
    const id = e.currentTarget.dataset.id
    wx.request({
      url: `${app.globalData.baseUrl}/api/claims/${id}/confirm-receive`,
      method: 'PUT',
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) { wx.showToast({ title: '已确认收到', icon: 'success' }); this.setData({ page: 1 }); this.loadClaims() }
        else wx.showToast({ title: res.data.message || '操作失败', icon: 'none' })
      },
      fail: () => wx.showToast({ title: '网络错误', icon: 'none' })
    })
  },
  // 点击跳转帖子详情
  goDetail(e) { wx.navigateTo({ url: `/pages/detail/detail?id=${e.currentTarget.dataset.postId}` }) }
})
