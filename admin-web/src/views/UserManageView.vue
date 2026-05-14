<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const users = ref<any[]>([])
const page = ref(1)
const total = ref(0)

onMounted(() => loadUsers())

async function loadUsers() {
  const res = await api.get('/api/admin/users', { params: { page: page.value, size: 10 } })
  users.value = res.data.data.records || []
  total.value = res.data.data.total || 0
}

async function banUser(id: number) {
  const res = await api.put(`/api/admin/users/${id}/ban`)
  if (res.data.code === 200) {
    ElMessage.success('已封禁')
    loadUsers()
  }
}

async function unbanUser(id: number) {
  const res = await api.put(`/api/admin/users/${id}/unban`)
  if (res.data.code === 200) {
    ElMessage.success('已解封')
    loadUsers()
  }
}
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">用户管理</h2>

    <el-table :data="users" stripe>
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'NORMAL'" type="success" size="small">正常</el-tag>
          <el-tag v-else type="danger" size="small">封禁</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button v-if="row.status === 'NORMAL'" size="small" type="danger" @click="banUser(row.id)">封禁</el-button>
          <el-button v-else size="small" type="success" @click="unbanUser(row.id)">解封</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" :page-size="10" @current-change="loadUsers" background layout="prev, pager, next" />
  </div>
</template>
