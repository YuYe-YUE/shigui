// 应用全局：初始化 Token 并提供工具方法
App({
  globalData: {
    token: '',
    userInfo: null,
    baseUrl: 'http://8.163.15.154'
  },

  // 启动时从本地存储恢复登录 Token
  onLaunch() {
    const token = wx.getStorageSync('token')
    if ( token) {
      this.globalData.token = token
    }
  },

  // 解析图片地址：相对路径拼接 baseUrl，绝对路径直接返回
  resolveImageUrl(url) {
    if (!url) return ''
    if (url.startsWith('http://') || url.startsWith('https://')) return url
    return this.globalData.baseUrl + url
  }
})
