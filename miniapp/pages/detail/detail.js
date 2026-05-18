// 帖子详情页：展示完整信息，支持认领申请、联系和图片预览
const app = getApp()
Page({
  data: { post: null, id: '' },
  // 加载页面，根据 id 参数拉取帖子详情
  onLoad(options) { this.setData({ id: options.id }); this.loadDetail() },
  // 从后端获取帖子详情，解析图片地址
  loadDetail() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/posts/${this.data.id}`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          const post = res.data.data
          if (post.imageUrls) post.imageUrls = post.imageUrls.map(u => app.resolveImageUrl(u))
          if (post.coverImageUrl) post.coverImageUrl = app.resolveImageUrl(post.coverImageUrl)
          this.setData({ post })
        }
      }
    })
  },
  // 申请认领：弹窗输入私密特征后提交
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
  // 打开聊天页面
  openChat() {
    if (!app.globalData.token) { wx.showToast({ title: '请先登录', icon: 'none' }); return }
    wx.navigateTo({ url: `/pages/chat/chat?postId=${this.data.id}` })
  },
  // 预览帖子图片
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
