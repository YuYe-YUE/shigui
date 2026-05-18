<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import api from '../api'

const matches = ref<any[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)

onMounted(() => loadMatches())

// 加载匹配记录列表：分页请求后端匹配数据。
async function loadMatches() {
  loading.value = true
  try {
    const res = await api.get('/api/admin/matches', { params: { page: page.value, size: 10 } })
    matches.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } catch {
    /* interceptor shows error */
  } finally {
    loading.value = false
  }
}

// 查看单据详情：弹窗展示失物单或招领单的完整信息。
async function viewPost(id: number, title: string) {
  try {
    const res = await api.get(`/api/admin/posts/${id}`)
    const post = res.data.data
    ElMessageBox.alert(
      `类型：${post.postType === 'LOST' ? '寻物' : '招领'}\n品类：${post.itemCategory || '无'}\n地点：${post.locationName || '无'}\n描述：${post.description || '无'}\n\n私密特征：${post.privateFeature || '无'}\n暂存地点：${post.storageLocation || '无'}`,
      title || post.title || '单据详情',
      { confirmButtonText: '关闭' }
    )
  } catch {
    /* interceptor shows error */
  }
}
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">匹配结果</h2>
    <el-table v-loading="loading" :data="matches" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="失物单" min-width="180">
        <template #default="{ row }">
          <div style="font-weight:600">{{ row.lostTitle || '无标题' }}</div>
          <div style="font-size:12px;color:#777">{{ row.lostItemName || '无物品名' }} · {{ row.lostCampusArea || '未知校区' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="招领单" min-width="180">
        <template #default="{ row }">
          <div style="font-weight:600">{{ row.foundTitle || '无标题' }}</div>
          <div style="font-size:12px;color:#777">{{ row.foundItemName || '无物品名' }} · {{ row.foundCampusArea || '未知校区' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="分数" width="90" />
      <el-table-column prop="reason" label="匹配原因" min-width="220" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewPost(row.lostPostId, row.lostTitle)">失物单</el-button>
          <el-button size="small" @click="viewPost(row.foundPostId, row.foundTitle)">招领单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      v-model:current-page="page"
      :total="total"
      :page-size="10"
      @current-change="loadMatches"
      background
      layout="prev, pager, next"
    />
  </div>
</template>
