// 个人中心页：用户信息、登录、以及各项功能入口
const app = getApp()

Page({
  data: {
    userInfo: null
  },

  // 每次页面显示时刷新用户信息
  onShow() {
    this.loadUserInfo()
  },

  // 从后端获取当前登录用户的信息
  loadUserInfo() {
    const token = app.globalData.token
    if (!token) {
      this.setData({ userInfo: null })
      return
    }
    wx.request({
      url: `${app.globalData.baseUrl}/api/user/me`,
      header: { satoken: token },
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ userInfo: res.data.data })
        } else {
          this.setData({ userInfo: null })
        }
      },
      fail: () => this.setData({ userInfo: null })
    })
  },

  // 微信登录：获取 code 后调用后端接口完成登录
  wxLogin() {
    wx.login({
      success: (res) => {
        const openid = 'dev_' + res.code
        wx.request({
          url: `${app.globalData.baseUrl}/api/user/wx-login`,
          method: 'POST',
          data: { openid },
          success: (resp) => {
            if (resp.data.code === 200) {
              const token = resp.data.data
              app.globalData.token = token
              wx.setStorageSync('token', token)
              this.loadUserInfo()
              wx.showToast({ title: '登录成功', icon: 'success' })
            }
          }
        })
      }
    })
  },

  // 跳转「我的记录」
  goMyPosts() {
    wx.navigateTo({ url: '/pages/my-posts/my-posts' })
  },

  // 跳转「我的寻物」
  goMyLostPosts() {
    wx.navigateTo({ url: '/pages/my-posts/my-posts?type=lost' })
  },

  // 跳转「我的招领」
  goMyFoundPosts() {
    wx.navigateTo({ url: '/pages/my-posts/my-posts?type=found' })
  },

  // 跳转匹配列表
  goMatches() {
    wx.navigateTo({ url: '/pages/matches/matches' })
  },

  // 跳转通知列表
  goNotifications() {
    wx.navigateTo({ url: '/pages/notifications/notifications' })
  },

  // 跳转认领记录
  goClaims() { wx.navigateTo({ url: '/pages/claims/claims' }) },

  // 未实现功能的占位提示
  onTapStub(e) {
    wx.showToast({ title: e.currentTarget.dataset.msg, icon: 'none' })
  }
})
