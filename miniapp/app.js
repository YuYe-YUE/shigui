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
  },

  resolveImageUrl(url) {
    if (!url) return ''
    if (url.startsWith('http://') || url.startsWith('https://')) return url
    return this.globalData.baseUrl + url
  }
})
