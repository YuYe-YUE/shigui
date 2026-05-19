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

// 仪表盘统计卡片配置，增加图标与颜色信息
const cards = computed(() => [
  { title: '注册用户', value: stats.value.registeredUsers, icon: '👤', color: '#3b82f6', bg: '#eff6ff' },
  { title: '匹配中单据', value: stats.value.matchingPosts, icon: '🔍', color: '#f59e0b', bg: '#fef3c7' },
  { title: '今日发布', value: stats.value.todayPublished, icon: '📝', color: '#10b981', bg: '#ecfdf5' },
  { title: '成功认领', value: stats.value.successfulClaims, icon: '✅', color: '#8b5cf6', bg: '#f5f3ff' },
  { title: '匹配记录', value: stats.value.matchRecords, icon: '🔗', color: '#ec4899', bg: '#fdf2f8' },
])

onMounted(() => loadDashboard())

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
  <div class="dashboard-container">
    <div class="dashboard-header">
      <h2 class="page-title">系统概览</h2>
      <p class="page-subtitle">欢迎回来，这是拾归系统的实时运行数据。</p>
    </div>
    
    <el-row v-loading="loading" :gutter="20" class="stat-row">
      <el-col v-for="card in cards" :key="card.title" :xs="24" :sm="12" :md="8" :lg="4" class="stat-col">
        <div class="stat-card">
          <div class="stat-icon-wrapper" :style="{ backgroundColor: card.bg, color: card.color }">
            <span class="stat-icon">{{ card.icon }}</span>
          </div>
          <div class="stat-info">
            <div class="stat-title">{{ card.title }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding-bottom: 24px;
}

.dashboard-header {
  margin-bottom: 32px;
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.page-subtitle {
  margin: 0;
  color: #64748b;
  font-size: 15px;
}

.stat-row {
  margin-bottom: 24px;
}

.stat-col {
  margin-bottom: 20px;
}

.stat-card {
  background: #ffffff;
  border-radius: var(--sg-radius);
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: var(--sg-shadow-sm);
  transition: var(--sg-transition);
  border: 1px solid rgba(0, 0, 0, 0.04);
  height: 100%;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: var(--sg-primary);
  opacity: 0;
  transition: var(--sg-transition);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--sg-shadow-hover);
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.stat-icon {
  font-size: 24px;
  line-height: 1;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-title {
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}
</style>
