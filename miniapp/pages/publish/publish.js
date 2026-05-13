Page({
  chooseType(event) {
    const type = event.currentTarget.dataset.type
    wx.navigateTo({
      url: `/pages/publish-form/publish-form?type=${type}`
    })
  }
})
