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
      manualLocationName: '',
      pickedLocationName: '',
      longitude: null,
      latitude: null,
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
    const nextData = { [`form.${field}`]: value }
    if (field === 'locationName') {
      nextData['form.manualLocationName'] = value
    }
    this.setData(nextData)
  },

  selectCategory(e) {
    const idx = e.detail.value
    this.setData({ 'form.itemCategory': this.data.categories[idx] })
  },

  chooseFoundLocation() {
    wx.chooseLocation({
      success: (res) => {
        const currentLocationName = this.data.form.locationName
        const currentPickedLocationName = this.data.form.pickedLocationName
        const pickedLocationName = res.name || res.address || currentLocationName || ''
        const nextData = {
          'form.locationName': pickedLocationName,
          'form.pickedLocationName': pickedLocationName,
          'form.longitude': res.longitude,
          'form.latitude': res.latitude
        }
        if (
          currentLocationName &&
          currentLocationName !== currentPickedLocationName &&
          currentLocationName !== pickedLocationName
        ) {
          nextData['form.manualLocationName'] = currentLocationName
        }
        this.setData(nextData)
      },
      fail: () => {
        wx.showToast({ title: '未标注地图位置，单据不会显示在地图中', icon: 'none' })
      }
    })
  },

  clearFoundLocation() {
    const manualLocationName = this.data.form.manualLocationName
    const shouldClearLocationName = this.data.form.locationName === this.data.form.pickedLocationName
    this.setData({
      'form.locationName': manualLocationName || (shouldClearLocationName ? '' : this.data.form.locationName),
      'form.manualLocationName': manualLocationName || '',
      'form.pickedLocationName': '',
      'form.longitude': null,
      'form.latitude': null
    })
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
    if (this.data.postType === 'FOUND' && (!this.data.form.longitude || !this.data.form.latitude)) {
      wx.showToast({ title: '未标注地图位置，单据不会显示在地图中', icon: 'none' })
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
      longitude: this.data.postType === 'FOUND' ? this.data.form.longitude : null,
      latitude: this.data.postType === 'FOUND' ? this.data.form.latitude : null,
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
