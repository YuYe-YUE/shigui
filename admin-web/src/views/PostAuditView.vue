<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const posts = ref<any[]>([])
const activeTab = ref('PENDING_AUDIT')
const page = ref(1)
const total = ref(0)
const loading = ref(false)

const detailVisible = ref(false)
const detailPost = ref<any>(null)

onMounted(() => loadPosts())

function switchTab(tab: string) {
  activeTab.value = tab
  page.value = 1
  loadPosts()
}

async function loadPosts() {
  loading.value = true
  try {
    const status = activeTab.value === 'all' ? undefined : activeTab.value
    const res = await api.get('/api/admin/posts', { params: { page: page.value, size: 10, status } })
    posts.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } catch {
    /* ElMessage.error already shown by interceptor */
  } finally {
    loading.value = false
  }
}

async function viewDetail(id: number) {
  try {
    const res = await api.get(`/api/admin/posts/${id}`)
    detailPost.value = res.data.data
    detailVisible.value = true
  } catch {
    /* error already shown */
  }
}

async function approve(id: number) {
  try {
    await ElMessageBox.confirm('确认审核通过该单据？通过后将进入匹配池。', '审核通过', { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'success' })
    await api.post(`/api/admin/posts/${id}/approve`)
    ElMessage.success('审核通过')
    loadPosts()
  } catch { /* 用户取消或接口错误 */ }
}

async function deletePost(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该单据？删除后不可恢复。', '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    const { value: reason } = await ElMessageBox.prompt('请填写删除原因', '删除原因', { confirmButtonText: '确认删除', cancelButtonText: '取消', inputType: 'textarea' })
    await api.delete(`/api/admin/posts/${id}`, { data: { reason } })
    ElMessage.success('已删除')
    loadPosts()
  } catch { /* 用户取消或接口错误 */ }
}
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">内容审核</h2>
    <el-tabs v-model="activeTab" @tab-click="(t: any) => switchTab(t.paneName as string)">
      <el-tab-pane label="待审核" name="PENDING_AUDIT" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <el-table :data="posts" stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="itemCategory" label="品类" width="80" />
      <el-table-column prop="postType" label="类型" width="70">
        <template #default="{ row }">{{ row.postType === 'LOST' ? '寻物' : '招领' }}</template>
      </el-table-column>
      <el-table-column prop="locationName" label="地点" width="120" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'PENDING_AUDIT'" type="warning" size="small">待审核</el-tag>
          <el-tag v-else-if="row.status === 'MATCHING'" type="success" size="small">匹配中</el-tag>
          <el-tag v-else size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row.id)">详情</el-button>
          <el-button v-if="row.status === 'PENDING_AUDIT'" size="small" type="success" @click="approve(row.id)">通过</el-button>
          <el-button size="small" type="danger" @click="deletePost(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" :page-size="10" @current-change="loadPosts" background layout="prev, pager, next" />

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
