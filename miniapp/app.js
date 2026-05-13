App({
  globalData: {
    token: '',
    userInfo: null,
    baseUrl: 'http://127.0.0.1:8080'
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  }
})
