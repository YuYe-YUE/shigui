<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const menuItems = [
  { path: '/dashboard', title: '仪表盘' },
  { path: '/posts', title: '内容审核' },
  { path: '/matches', title: '匹配结果' },
  { path: '/claims', title: '认领审核' },
  { path: '/users', title: '用户管理' },
]
</script>

<template>
  <el-container class="layout-container">
    <el-aside width="240px" class="layout-sidebar">
      <div class="sidebar-logo">
        <span class="logo-text">拾归 · 管理后台</span>
      </div>
      <el-menu
        :default-active="$route.path"
        class="sidebar-menu"
        background-color="transparent"
        text-color="rgba(255, 255, 255, 0.65)"
        active-text-color="#ffffff"
        @select="(path: string) => router.push(path)"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path" class="menu-item-custom">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <el-button class="logout-btn" text @click="auth.logout()">退出登录</el-button>
      </el-header>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
  background-color: var(--sg-bg-light);
}

.layout-sidebar {
  background: linear-gradient(180deg, #064e3b 0%, #022c22 100%);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  z-index: 10;
}

.sidebar-logo {
  padding: 24px 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-text {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
}

.sidebar-menu {
  border-right: none;
  margin-top: 16px;
}

.menu-item-custom {
  margin: 4px 12px;
  border-radius: 8px;
  transition: var(--sg-transition);
}

.menu-item-custom:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

.menu-item-custom.is-active {
  background-color: rgba(255, 255, 255, 0.15) !important;
  font-weight: 600;
}

.layout-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 24px;
  box-shadow: var(--sg-shadow-sm);
  z-index: 5;
}

.logout-btn {
  font-weight: 500;
  color: #475569;
  transition: var(--sg-transition);
}

.logout-btn:hover {
  color: var(--sg-primary);
  background-color: rgba(5, 150, 105, 0.1);
}

.layout-main {
  background-color: transparent;
  padding: 24px;
  overflow-y: auto;
}
</style>
