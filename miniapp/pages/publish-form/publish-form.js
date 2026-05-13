const app = getApp()

Page({
  data: {
    postType: 'LOST',
    categories: ['校园卡', '学生证', '钥匙', '耳机', '水杯', '雨伞', '书籍', '其他'],
    form: {
      itemName: '',
      itemCategory: '',
      description: '',
      privateFeature: '',
      campusArea: '',
      locationName: '',
      eventTime: '',
      storageLocation: ''
    },
    submitting: false
  },

  onLoad(options) {
    const type = options.type === 'found' ? 'FOUND' : 'LOST'
    this.setData({ postType: type })
    wx.setNavigationBarTitle({
      title: type === 'LOST' ? '发布寻物启事' : '发布招领启事'
    })
  },

  setField(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    this.setData({ [`form.${field}`]: value })
  },

  selectCategory(e) {
    const idx = e.detail.value
    this.setData({ 'form.itemCategory': this.data.categories[idx] })
  },

  submit() {
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

    const payload = {
      postType: this.data.postType,
      title: this.data.form.itemName,
      itemName: this.data.form.itemName,
      itemCategory: this.data.form.itemCategory,
      description: this.data.form.description,
      privateFeature: this.data.form.privateFeature,
      campusArea: this.data.form.campusArea,
      locationName: this.data.form.locationName,
      storageLocation: this.data.postType === 'FOUND' ? this.data.form.storageLocation : '',
      eventTime: this.data.form.eventTime ? `${this.data.form.eventTime}T00:00:00` : ''
    }

    wx.request({
      url: `${app.globalData.baseUrl}/api/posts`,
      method: 'POST',
      header: { satoken: token },
      data: payload,
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
    const f = this.data.form
    if (!f.itemName.trim()) return '请填写物品名称'
    if (!f.itemCategory.trim()) return '请选择物品类别'
    if (!f.campusArea.trim()) return '请填写校区'
    if (!f.locationName.trim()) return '请填写地点'
    if (!f.eventTime.trim()) return '请选择时间'
    return ''
  }
})
