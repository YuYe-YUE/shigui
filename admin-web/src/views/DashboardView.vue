<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import api from '../api'

const stats = ref({
  registeredUsers: 0,
  matchingPosts: 0,
  todayPublished: 0,
  successfulClaims: 0,
  matchRecords: 0,
})
const loading = ref(false)

// 仪表盘统计卡片配置：由后端返回的 stats 数据动态生成。
const cards = computed(() => [
  { title: '注册用户', value: stats.value.registeredUsers },
  { title: '匹配中单据', value: stats.value.matchingPosts },
  { title: '今日发布', value: stats.value.todayPublished },
  { title: '成功认领', value: stats.value.successfulClaims },
  { title: '匹配记录', value: stats.value.matchRecords },
])

onMounted(() => loadDashboard())

// 加载仪表盘统计数据：从 /api/admin/dashboard 获取五个核心指标。
async function loadDashboard() {
  loading.value = true
  try {
    const res = await api.get('/api/admin/dashboard')
    stats.value = res.data.data
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">仪表盘</h2>
    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="card in cards" :key="card.title" :xs="24" :sm="12" :md="8" :lg="5">
        <el-card shadow="hover" style="margin-bottom:16px">
          <div style="font-size:14px;color:#666">{{ card.title }}</div>
          <div style="font-size:28px;font-weight:700;margin-top:8px">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
