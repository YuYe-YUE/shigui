const app = getApp()
Page({
  data: { claims: [], page: 1 },
  onShow() { this.setData({ page: 1 }); this.loadClaims() },
  onReachBottom() { this.setData({ page: this.data.page + 1 }); this.loadClaims() },
  loadClaims() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/claims/mine?page=${this.data.page}&size=10`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          const records = res.data.data.records || []
          this.setData({ claims: this.data.page === 1 ? records : [...this.data.claims, ...records] })
        }
      }
    })
  },
  confirmReceive(e) {
    const id = e.currentTarget.dataset.id
    wx.request({
      url: `${app.globalData.baseUrl}/api/claims/${id}/confirm-receive`,
      method: 'PUT',
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) { wx.showToast({ title: '已确认收到', icon: 'success' }); this.setData({ page: 1 }); this.loadClaims() }
        else wx.showToast({ title: res.data.message || '操作失败', icon: 'none' })
      }
    })
  },
  goDetail(e) { wx.navigateTo({ url: `/pages/detail/detail?id=${e.currentTarget.dataset.postId}` }) }
})
