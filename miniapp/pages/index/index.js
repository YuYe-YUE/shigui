// 首页：展示失物/招领列表，支持分类筛选、关键词搜索和分页加载
const app = getApp()

Page({
  data: {
    posts: [],
    categories: ['全部', '校园卡', '学生证', '钥匙', '耳机', '水杯', '雨伞', '书籍', '其他'],
    activeCategory: '全部',
    activeTab: 'all',
    page: 1,
    keyword: ''
  },

  // 页面加载时拉取帖子列表
  onLoad() {
    this.loadPosts()
  },

  // 下拉刷新：重置页码并重新加载
  onPullDownRefresh() {
    this.setData({ page: 1, posts: [] })
    this.loadPosts().then(() => wx.stopPullDownRefresh())
  },

  // 触底加载：页码递增后追加数据
  onReachBottom() {
    this.setData({ page: this.data.page + 1 })
    this.loadPosts()
  },

  // 按当前筛选条件请求帖子列表，支持分页
  loadPosts() {
    const { activeCategory, activeTab, page, keyword } = this.data
    const token = app.globalData.token
    let url = `${app.globalData.baseUrl}/api/posts?page=${page}&size=10`
    if (activeTab !== 'all') url += `&postType=${activeTab.toUpperCase()}`
    if (activeCategory !== '全部') url += `&itemCategory=${activeCategory}`
    if (keyword.trim()) url += `&keyword=${encodeURIComponent(keyword.trim())}`

    return new Promise((resolve) => {
      wx.request({
        url,
        header: token ? { satoken: token } : {},
        success: (res) => {
          if (res.data.code === 200) {
            const newPosts = (res.data.data.records || []).map(p => {
              if (p.coverImageUrl) p.coverImageUrl = app.resolveImageUrl(p.coverImageUrl)
              if (p.eventTime) p.eventTime = p.eventTime.split('T')[0]
              if (p.publishedAt) p.publishedAt = p.publishedAt.split('T')[0]
              return p
            })
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

  // 切换失物/招领/全部 Tab
  switchTab(e) {
    const nextTab = e.currentTarget.dataset.tab
    const finalTab = (nextTab === this.data.activeTab) ? 'all' : nextTab
    this.setData({ activeTab: finalTab, page: 1, posts: [] })
    this.loadPosts()
  },

  // 切换物品分类
  switchCategory(e) {
    const cat = e.currentTarget.dataset.category
    this.setData({ activeCategory: cat, page: 1, posts: [] })
    this.loadPosts()
  },

  // 搜索框输入时更新关键词
  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  // 确认搜索时重新加载列表
  onSearchConfirm() {
    this.setData({ page: 1, posts: [] })
    this.loadPosts()
  },

  // 点击帖子跳转详情页
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  },

  // 跳转地图视图
  goMap() {
    wx.navigateTo({ url: '/pages/map/map' })
  }
})
