// 我的记录页：展示当前用户发布的寻物/招领帖子列表
const app = getApp()

Page({
  data: {
    posts: [],
    page: 1,
    postType: '',   // 'LOST' | 'FOUND' | '' (all)
    title: '我的记录'
  },

  // 加载页面，根据 type 参数筛选寻物/招领/全部
  onLoad(options) {
    const type = (options && options.type) || ''
    this.setData({
      postType: type.toUpperCase(),
      title: type === 'lost' ? '我的寻物' : type === 'found' ? '我的招领' : '我的记录'
    })
    wx.setNavigationBarTitle({ title: this.data.title })
    this.loadPosts()
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({ page: 1, posts: [] })
    this.loadPosts().then(() => wx.stopPullDownRefresh())
  },

  // 触底翻页
  onReachBottom() {
    this.setData({ page: this.data.page + 1 })
    this.loadPosts()
  },

  // 请求当前用户的帖子列表，支持按类型筛选和分页
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

  // 点击帖子跳转详情页
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  }
})
