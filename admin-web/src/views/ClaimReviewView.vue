<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const claims = ref<any[]>([])
const page = ref(1)
const total = ref(0)
const status = ref('PENDING_ADMIN_REVIEW')
const loading = ref(false)

// 认领状态筛选选项，对应后端 claim_record.status 枚举值。
const statusOptions = [
  { label: '待人工审核', value: 'PENDING_ADMIN_REVIEW' },
  { label: 'AI 审核中', value: 'PENDING_AI_REVIEW' },
  { label: '已通过', value: 'VERIFIED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '全部', value: '' },
]

onMounted(() => loadClaims())

// 加载认领列表：按状态和分页参数请求后端。
async function loadClaims() {
  loading.value = true
  try {
    const res = await api.get('/api/admin/claims', {
      params: { page: page.value, size: 10, status: status.value || undefined },
    })
    claims.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } finally {
    loading.value = false
  }
}

// 切换筛选状态时重置页码并重新加载数据。
function resetPageAndLoad() {
  page.value = 1
  loadClaims()
}

// 通过认领申请：调审批接口后刷新列表。
async function approve(row: any) {
  await api.put(`/api/admin/claims/${row.id}/approve`)
  ElMessage.success('已通过认领申请')
  loadClaims()
}

// 拒绝认领申请：弹窗输入拒绝原因后调接口。
async function reject(row: any) {
  const reason = await ElMessageBox.prompt('请输入拒绝原因', '拒绝认领', {
    confirmButtonText: '拒绝',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '原因不能为空',
  })
  await api.put(`/api/admin/claims/${row.id}/reject`, { reason: reason.value })
  ElMessage.success('已拒绝认领申请')
  loadClaims()
}
</script>

<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
      <h2 style="margin:0">认领审核</h2>
      <el-select v-model="status" style="width:160px" @change="resetPageAndLoad">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="claims" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="单据" min-width="180">
        <template #default="{ row }">
          <div style="font-weight:600">{{ row.postTitle || '无标题' }}</div>
          <div style="font-size:12px;color:#777">{{ row.itemName || '无物品名' }} · {{ row.campusArea || '未知校区' }} {{ row.locationName || '' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="150" />
      <el-table-column prop="privateFeature" label="单据私密特征" min-width="180" show-overflow-tooltip />
      <el-table-column prop="privateFeatureAnswer" label="认领答案" min-width="180" show-overflow-tooltip />
      <el-table-column prop="aiDecision" label="AI 结论" width="110" />
      <el-table-column prop="aiConfidence" label="置信度" width="90" />
      <el-table-column prop="aiReason" label="AI 理由" min-width="180" show-overflow-tooltip />
      <el-table-column prop="adminReason" label="管理员理由" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="申请时间" width="180" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="primary"
            :disabled="!['PENDING_AI_REVIEW', 'PENDING_ADMIN_REVIEW'].includes(row.status)"
            @click="approve(row)"
          >
            通过
          </el-button>
          <el-button
            size="small"
            type="danger"
            :disabled="!['PENDING_AI_REVIEW', 'PENDING_ADMIN_REVIEW'].includes(row.status)"
            @click="reject(row)"
          >
            拒绝
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      v-model:current-page="page"
      :total="total"
      :page-size="10"
      @current-change="loadClaims"
      background
      layout="prev, pager, next"
    />
  </div>
</template>
