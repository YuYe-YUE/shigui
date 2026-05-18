// 聊天页：创建或恢复会话，支持发送和拉取消息
const app = getApp()
Page({
  data: { sessionId: '', messages: [], inputText: '' },
  // 页面加载，根据 postId 创建或打开聊天会话
  onLoad(options) {
    this.setData({ postId: options.postId })
    this.initSession()
  },
  // 创建新会话或获取已有会话 ID
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
  // 加载会话中的历史消息
  loadMessages() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/chat/sessions/${this.data.sessionId}/messages`,
      header: { satoken: app.globalData.token },
      success: (res) => { if (res.data.code === 200) this.setData({ messages: res.data.data }) }
    })
  },
  // 输入框内容变更
  setInput(e) { this.setData({ inputText: e.detail.value }) },
  // 发送消息，发送后刷新消息列表
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
