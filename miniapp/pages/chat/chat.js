// 聊天页：创建或恢复会话，支持发送和拉取消息
const app = getApp()
Page({
  data: { sessionId: '', messages: [], inputText: '', loading: true },

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
        if (res.data.code === 200) {
          this.setData({ sessionId: res.data.data.id })
          this.loadMessages()
        } else {
          this.setData({ loading: false })
          wx.showToast({ title: res.data.message || '创建会话失败', icon: 'none' })
        }
      },
      fail: () => {
        this.setData({ loading: false })
        wx.showToast({ title: '网络请求失败', icon: 'none' })
      }
    })
  },

  // 加载会话中的历史消息
  loadMessages() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/chat/sessions/${this.data.sessionId}/messages`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        this.setData({ loading: false })
        if (res.data.code === 200) {
          this.setData({ messages: res.data.data })
        }
      },
      fail: () => {
        this.setData({ loading: false })
        wx.showToast({ title: '加载消息失败', icon: 'none' })
      }
    })
  },

  setInput(e) { this.setData({ inputText: e.detail.value }) },

  // 发送消息，发送后刷新消息列表
  send() {
    if (!this.data.inputText.trim()) return
    wx.request({
      url: `${app.globalData.baseUrl}/api/chat/sessions/${this.data.sessionId}/messages`, method: 'POST',
      header: { satoken: app.globalData.token },
      data: { content: this.data.inputText },
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ inputText: '' })
          this.loadMessages()
        } else {
          wx.showToast({ title: res.data.message || '发送失败', icon: 'none' })
        }
      },
      fail: () => wx.showToast({ title: '发送失败，请检查网络', icon: 'none' })
    })
  }
})
