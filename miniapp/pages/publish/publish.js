Page({
  goPublish(e) {
    const type = e.currentTarget.dataset.type
    wx.navigateTo({ url: `/pages/publish-form/publish-form?type=${type}` })
  }
})
