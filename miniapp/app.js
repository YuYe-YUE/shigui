App({
  globalData: {
    // token 放在全局数据中，方便各页面请求后端时统一读取。
    token: '',
    userInfo: null,
    baseUrl: 'http://127.0.0.1:8080'
  },

  onLaunch() {
    // 小程序冷启动时从本地缓存恢复登录态。
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  }
})
