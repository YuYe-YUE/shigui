// 发布入口页：选择「寻物」或「招领」类型后跳转表单页
Page({
  // 点击按钮跳转发布表单页，携带 lost/found 类型参数
  goPublish(e) {
    const type = e.currentTarget.dataset.type
    wx.navigateTo({ url: `/pages/publish-form/publish-form?type=${type}` })
  }
})
