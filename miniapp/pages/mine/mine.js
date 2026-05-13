const app = getApp()

Page({
  data: {
    userInfo: null
  },

  onShow() {
    this.loadUserInfo()
  },

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

  goMyPosts() {
    wx.navigateTo({ url: '/pages/index/index?tab=mine' })
  }
})
