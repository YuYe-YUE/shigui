const app = getApp()
Page({
  data: { sessionId: '', messages: [], inputText: '', myId: 0 },
  onLoad(options) {
    this.setData({ postId: options.postId })
    this.loadCurrentUser()
    this.initSession()
  },
  loadCurrentUser() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/user/me`,
      header: { satoken: app.globalData.token },
      success: (res) => { if (res.data.code === 200 && res.data.data) this.setData({ myId: res.data.data.id }) }
    })
  },
  initSession() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/chat/sessions`, method: 'POST',
      header: { satoken: app.globalData.token },
      data: { postId: this.data.postId },
      success: (res) => {
        if (res.data.code === 200) { this.setData({ sessionId: res.data.data.id }); this.loadMessages() }
      }
    })
  },
  loadMessages() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/chat/sessions/${this.data.sessionId}/messages`,
      header: { satoken: app.globalData.token },
      success: (res) => { if (res.data.code === 200) this.setData({ messages: res.data.data }) }
    })
  },
  setInput(e) { this.setData({ inputText: e.detail.value }) },
  send() {
    if (!this.data.inputText.trim()) return
    wx.request({
      url: `${app.globalData.baseUrl}/api/chat/sessions/${this.data.sessionId}/messages`, method: 'POST',
      header: { satoken: app.globalData.token },
      data: { content: this.data.inputText },
      success: (res) => {
        if (res.data.code === 200) { this.setData({ inputText: '' }); this.loadMessages() }
      }
    })
  }
})
