const app = getApp()
const EAST_CAMPUS_COORDS = {
  latitude: 23.06,
  longitude: 113.39
}

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
      storageLocation: '',
      imageFiles: [],
      imageUrls: []
    },
    submitting: false,
    uploadingImages: false
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
      if (this.data.form.pickedLocationName) {
        nextData['form.pickedLocationName'] = ''
        nextData['form.longitude'] = null
        nextData['form.latitude'] = null
      }
    }
    this.setData(nextData)
  },

  selectCategory(e) {
    const idx = e.detail.value
    this.setData({ 'form.itemCategory': this.data.categories[idx] })
  },

  chooseFoundLocation() {
    wx.chooseLocation({
      latitude: EAST_CAMPUS_COORDS.latitude,
      longitude: EAST_CAMPUS_COORDS.longitude,
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
      fail: (error) => {
        const errMsg = (error && error.errMsg) || ''
        if (errMsg.includes('cancel')) {
          return
        }
        console.warn('chooseLocation failed', errMsg)
        wx.showToast({ title: '打开地图失败，请用真机预览后重试', icon: 'none' })
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

  chooseImages() {
    const currentFiles = this.data.form.imageFiles || []
    const remaining = 3 - currentFiles.length
    if (remaining <= 0) {
      wx.showToast({ title: '最多上传 3 张图片', icon: 'none' })
      return
    }
    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      success: (res) => {
        const selectedFiles = (res.tempFiles || []).slice(0, remaining)
        const nextFiles = currentFiles.concat(selectedFiles)
        this.setData({
          'form.imageFiles': nextFiles
        })
        if ((res.tempFiles || []).length >= remaining) {
          wx.showToast({ title: '最多上传 3 张图片', icon: 'none' })
        }
      }
    })
  },

  removeImage(e) {
    const index = Number(e.currentTarget.dataset.index)
    const nextFiles = (this.data.form.imageFiles || []).filter((_, idx) => idx !== index)
    const nextUrls = (this.data.form.imageUrls || []).filter((_, idx) => idx !== index)
    this.setData({
      'form.imageFiles': nextFiles,
      'form.imageUrls': nextUrls
    })
  },

  previewImage(e) {
    const index = Number(e.currentTarget.dataset.index)
    const urls = (this.data.form.imageFiles || []).map((file) => file.tempFilePath)
    if (!urls.length) {
      return
    }
    wx.previewImage({
      current: urls[index] || urls[0],
      urls
    })
  },

  uploadImages(token) {
    const imageFiles = this.data.form.imageFiles || []
    if (!imageFiles.length) {
      this.setData({ 'form.imageUrls': [] })
      return Promise.resolve([])
    }
    this.setData({ uploadingImages: true })
    const uploads = imageFiles.map((file) => new Promise((resolve, reject) => {
      wx.uploadFile({
        url: `${app.globalData.baseUrl}/api/files/upload`,
        filePath: file.tempFilePath,
        name: 'file',
        header: { satoken: token },
        success: (res) => {
          try {
            const payload = JSON.parse(res.data || '{}')
            if (res.statusCode === 200 && payload.code === 200 && payload.data && payload.data.url) {
              resolve(payload.data.url)
              return
            }
            reject(new Error(payload.message || '图片上传失败'))
          } catch (error) {
            reject(new Error('图片上传失败'))
          }
        },
        fail: () => reject(new Error('图片上传失败'))
      })
    }))
    return Promise.all(uploads)
      .then((imageUrls) => {
        this.setData({ 'form.imageUrls': imageUrls })
        return imageUrls
      })
      .finally(() => this.setData({ uploadingImages: false }))
  },

  requestPublish(payload, token) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${app.globalData.baseUrl}/api/posts`,
        method: 'POST',
        header: { satoken: token },
        data: payload,
        success: (res) => {
          if (res.data.code === 200) {
            resolve(res)
            return
          }
          reject(new Error(res.data.message || '提交失败'))
        },
        fail: () => reject(new Error('网络错误')),
        complete: () => this.setData({ submitting: false })
      })
    })
  },

  async submit() {
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
    try {
      const imageUrls = await this.uploadImages(token)
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
        eventTime: this.data.form.eventTime ? `${this.data.form.eventTime}T00:00:00` : '',
        imageUrls
      }
      await this.requestPublish(payload, token)
      wx.showToast({ title: '提交成功，等待审核', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/mine/mine' }), 800)
    } catch (error) {
      this.setData({ submitting: false })
      wx.showToast({ title: error.message || '提交失败', icon: 'none' })
    }
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
