const app = getApp()

Page({
  data: {
    posts: [],
    categories: ['全部', '校园卡', '学生证', '钥匙', '耳机', '水杯', '雨伞', '书籍', '其他'],
    activeCategory: '全部',
    activeTab: 'all',
    page: 1,
    keyword: '',
    mode: 'public',   // 'public' | 'mine'
    myPostType: ''    // 'LOST' | 'FOUND' | ''
  },

  onLoad(options) {
    if (options && options.tab === 'mine') {
      this.setData({ mode: 'mine', myPostType: (options.type || '').toUpperCase() })
      if (options.type) {
        this.setData({ activeTab: options.type })
      }
    }
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
    const { activeCategory, activeTab, page, keyword, mode, myPostType } = this.data
    const token = app.globalData.token
    let url

    if (mode === 'mine') {
      url = `${app.globalData.baseUrl}/api/posts/mine?page=${page}&size=10`
      if (myPostType) url += `&postType=${myPostType}`
    } else {
      url = `${app.globalData.baseUrl}/api/posts?page=${page}&size=10`
      if (activeTab !== 'all') url += `&postType=${activeTab.toUpperCase()}`
      if (activeCategory !== '全部') url += `&itemCategory=${activeCategory}`
      if (keyword.trim()) url += `&keyword=${encodeURIComponent(keyword.trim())}`
    }

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

  switchTab(e) {
    const nextTab = e.currentTarget.dataset.tab
    const finalTab = (nextTab === this.data.activeTab) ? 'all' : nextTab
    this.setData({ activeTab: finalTab, page: 1, posts: [] })
    this.loadPosts()
  },

  switchCategory(e) {
    const cat = e.currentTarget.dataset.category
    this.setData({ activeCategory: cat, page: 1, posts: [] })
    this.loadPosts()
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearchConfirm() {
    this.setData({ page: 1, posts: [] })
    this.loadPosts()
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  },

  goMap() {
    wx.navigateTo({ url: '/pages/map/map' })
  }
})
