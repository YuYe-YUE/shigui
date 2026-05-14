const app = getApp()

Page({
  data: {
    posts: [],
    page: 1,
    postType: '',   // 'LOST' | 'FOUND' | '' (all)
    title: '我的记录'
  },

  onLoad(options) {
    const type = (options && options.type) || ''
    this.setData({
      postType: type.toUpperCase(),
      title: type === 'lost' ? '我的寻物' : type === 'found' ? '我的招领' : '我的记录'
    })
    wx.setNavigationBarTitle({ title: this.data.title })
    this.loadPosts()
  },

  onPullDownRefresh() {
    this.setData({ page: 1, posts: [] })
    this.loadPosts().then(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.setData({ page: this.data.page + 1 })
    this.loadPosts()
  },

  loadPosts() {
    const { page, postType } = this.data
    const token = app.globalData.token
    let url = `${app.globalData.baseUrl}/api/posts/mine?page=${page}&size=10`
    if (postType) url += `&postType=${postType}`

    return new Promise((resolve) => {
      wx.request({
        url,
        header: token ? { satoken: token } : {},
        success: (res) => {
          if (res.data.code === 200) {
            const newPosts = res.data.data.records || []
            this.setData({
              posts: page === 1 ? newPosts : [...this.data.posts, ...newPosts]
            })
          }
          resolve()
        },
        fail: () => resolve()
      })
    })
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  }
})
