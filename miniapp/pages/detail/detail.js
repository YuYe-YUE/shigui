const app = getApp()
Page({
  data: { post: null, id: '' },
  onLoad(options) { this.setData({ id: options.id }); this.loadDetail() },
  loadDetail() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/posts/${this.data.id}`,
      header: { satoken: app.globalData.token },
      success: (res) => { if (res.data.code === 200) this.setData({ post: res.data.data }) }
    })
  },
  applyClaim() {
    if (!app.globalData.token) { wx.showToast({ title: '请先登录', icon: 'none' }); return }
    wx.showModal({
      title: '申请认领', editable: true, placeholderText: '请填写只有物主知道的特征',
      success: (modal) => {
        if (!modal.confirm) return
        const answer = (modal.content || '').trim()
        if (!answer) { wx.showToast({ title: '请填写私密特征', icon: 'none' }); return }
        wx.request({
          url: `${app.globalData.baseUrl}/api/claims`, method: 'POST',
          header: { satoken: app.globalData.token },
          data: { postId: Number(this.data.id), privateFeatureAnswer: answer },
          success: (res) => {
            if (res.data.code === 200) {
              wx.showToast({ title: '已提交认领', icon: 'success' })
              wx.navigateTo({ url: '/pages/claims/claims' })
            } else wx.showToast({ title: res.data.message || '提交失败', icon: 'none' })
          },
          fail: () => wx.showToast({ title: '网络错误', icon: 'none' })
        })
      }
    })
  },
  openChat() {
    if (!app.globalData.token) { wx.showToast({ title: '请先登录', icon: 'none' }); return }
    wx.navigateTo({ url: `/pages/chat/chat?postId=${this.data.id}` })
  },
  previewImage(e) {
    const index = Number(e.currentTarget.dataset.index)
    const urls = (this.data.post && this.data.post.imageUrls) || []
    if (!urls.length) {
      return
    }
    wx.previewImage({
      current: urls[index] || urls[0],
      urls
    })
  }
})
