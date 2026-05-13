/**
 * 帖子详情页 - 查看帖子完整信息，支持认领申请和匿名聊天
 * 用户可在此页面查看失物招领帖子的完整信息，发起认领验证或与发布者匿名沟通
 */
const app = getApp()

Page({
  data: {
    post: null,
    id: ''
  },

  /**
   * 页面加载 - 接收帖子ID参数并加载详情数据
   */
  onLoad(options) {
    this.setData({ id: options.id })
    this.loadDetail()
  },

  /**
   * 请求帖子详情数据
   */
  loadDetail() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/posts/${this.data.id}`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ post: res.data.data })
        }
      }
    })
  },

  /**
   * 发起认领申请 - 弹窗要求用户填写物品的非公开特征以验证失主身份
   */
  applyClaim() {
    wx.showModal({
      title: '认领申请',
      editable: true,
      placeholderText: '请填写物品的非公开特征以验证身份',
      success: (res) => {
        if (res.confirm) {
          wx.request({
            url: `${app.globalData.baseUrl}/api/claims`,
            method: 'POST',
            header: { satoken: app.globalData.token },
            data: {
              postId: this.data.id,
              privateFeatureAnswer: res.content
            },
            success: (resp) => {
              if (resp.data.code === 200) {
                wx.showToast({ title: '申请已提交', icon: 'success' })
              }
            }
          })
        }
      }
    })
  },

  /**
   * 打开匿名聊天会话 - 跳转到与该帖子关联的聊天页面
   */
  openChat() {
    wx.navigateTo({ url: `/pages/chat/chat?postId=${this.data.id}` })
  }
})
