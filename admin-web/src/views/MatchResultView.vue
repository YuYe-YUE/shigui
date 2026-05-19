<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '../api'

const matches = ref<any[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)

const detailVisible = ref(false)
const detailPost = ref<any>(null)

onMounted(() => loadMatches())

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

async function viewPost(id: number) {
  try {
    const res = await api.get(`/api/admin/posts/${id}`)
    detailPost.value = res.data.data
    detailVisible.value = true
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
          <el-button size="small" @click="viewPost(row.lostPostId)">失物单</el-button>
          <el-button size="small" @click="viewPost(row.foundPostId)">招领单</el-button>
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

    <el-dialog v-model="detailVisible" :title="detailPost?.title || '单据详情'" width="640px">
      <template v-if="detailPost">
        <div v-if="detailPost.imageUrls && detailPost.imageUrls.length" style="display:flex;gap:8px;overflow-x:auto;margin-bottom:16px">
          <img v-for="(url, i) in detailPost.imageUrls" :key="i" :src="url" style="height:200px;border-radius:8px;object-fit:cover;flex-shrink:0" />
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="类型">{{ detailPost.postType === 'LOST' ? '寻物' : '招领' }}</el-descriptions-item>
          <el-descriptions-item label="品类">{{ detailPost.itemCategory || '无' }}</el-descriptions-item>
          <el-descriptions-item label="物品名称">{{ detailPost.itemName || '无' }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ detailPost.campusArea }} {{ detailPost.locationName }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ detailPost.eventTime || '无' }}</el-descriptions-item>
          <el-descriptions-item label="暂存地点">{{ detailPost.storageLocation || '无' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detailPost.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="私密特征" :span="2">{{ detailPost.privateFeature || '无' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>
