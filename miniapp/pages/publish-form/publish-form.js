const app = getApp()

Page({
  data: {
    postType: 'LOST',
    title: '',
    itemName: '',
    itemCategory: '',
    description: '',
    privateFeature: '',
    campusArea: '',
    locationName: '',
    storageLocation: '',
    eventTime: '',
    submitting: false
  },

  onLoad(options) {
    const type = options.type === 'FOUND' ? 'FOUND' : 'LOST'
    this.setData({ postType: type })
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ [field]: event.detail.value })
  },

  onDateChange(event) {
    this.setData({ eventTime: event.detail.value })
  },

  submitPost() {
    const token = app.globalData.token
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      wx.switchTab({ url: '/pages/mine/mine' })
      return
    }
    const error = this.validate()
    if (error) {
      wx.showToast({ title: error, icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    wx.request({
      url: `${app.globalData.baseUrl}/api/posts`,
      method: 'POST',
      header: { satoken: token },
      data: this.buildPayload(),
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({ title: '提交成功，等待审核', icon: 'success' })
          setTimeout(() => wx.switchTab({ url: '/pages/mine/mine' }), 800)
        } else {
          wx.showToast({ title: res.data.message || '提交失败', icon: 'none' })
        }
      },
      fail: () => wx.showToast({ title: '网络错误', icon: 'none' }),
      complete: () => this.setData({ submitting: false })
    })
  },

  validate() {
    if (!this.data.title.trim()) return '请填写标题'
    if (!this.data.itemName.trim()) return '请填写物品名称'
    if (!this.data.itemCategory.trim()) return '请填写物品分类'
    if (!this.data.campusArea.trim()) return '请填写校区'
    if (!this.data.locationName.trim()) return '请填写地点'
    if (!this.data.eventTime.trim()) return '请选择时间'
    return ''
  },

  buildPayload() {
    return {
      postType: this.data.postType,
      title: this.data.title,
      itemName: this.data.itemName,
      itemCategory: this.data.itemCategory,
      description: this.data.description,
      privateFeature: this.data.privateFeature,
      campusArea: this.data.campusArea,
      locationName: this.data.locationName,
      storageLocation: this.data.postType === 'FOUND' ? this.data.storageLocation : '',
      eventTime: `${this.data.eventTime}T00:00:00`
    }
  }
})
