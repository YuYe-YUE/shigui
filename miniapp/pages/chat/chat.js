/**
 * 匿名聊天页 - 失主与拾捡者之间的匿名即时通讯
 * 双方通过帖子关联的匿名会话进行沟通，保护双方真实身份信息
 */
const app = getApp()

Page({
  data: {
    sessionId: '',
    messages: [],
    inputText: '',
    myId: 0
  },

  /**
   * 页面加载 - 接收帖子ID并初始化聊天会话
   */
  onLoad(options) {
    this.setData({ postId: options.postId })
    this.loadCurrentUser()
    this.initSession()
  },

  /**
   * 加载当前用户 ID，用于区分消息左右气泡。
   */
  loadCurrentUser() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/user/me`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200 && res.data.data) {
          this.setData({ myId: res.data.data.id })
        }
      }
    })
  },

  /**
   * 初始化匿名聊天会话 - 根据帖子ID创建或获取已有的会话
   */
  initSession() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/chat/sessions`,
      method: 'POST',
      header: { satoken: app.globalData.token },
      data: { postId: this.data.postId },
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ sessionId: res.data.data.id })
          this.loadMessages()
        }
      }
    })
  },

  /**
   * 加载当前会话的历史消息记录
   */
  loadMessages() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/chat/sessions/${this.data.sessionId}/messages`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ messages: res.data.data })
        }
      }
    })
  },

  /**
   * 输入框内容变更事件
   */
  setInput(e) {
    this.setData({ inputText: e.detail.value })
  },

  /**
   * 发送消息 - 将输入内容发送到当前聊天会话
   */
  send() {
    if (!this.data.inputText.trim()) return
    wx.request({
      url: `${app.globalData.baseUrl}/api/chat/sessions/${this.data.sessionId}/messages`,
      method: 'POST',
      header: { satoken: app.globalData.token },
      data: { content: this.data.inputText },
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ inputText: '' })
          this.loadMessages()
        }
      }
    })
  }
})
