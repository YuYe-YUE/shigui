<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const menuItems = [
  { path: '/dashboard', title: '仪表盘' },
]
</script>

<template>
  <el-container style="height:100vh">
    <el-aside width="220px" style="background:#00573D">
      <div style="padding:20px;color:#fff;font-size:18px;font-weight:700;text-align:center">
        拾归 · 管理后台
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#00573D"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#fff"
        @select="(path: string) => router.push(path)"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff;border-bottom:1px solid #eee;display:flex;align-items:center;justify-content:flex-end;padding:0 20px">
        <el-button text @click="auth.logout()">退出登录</el-button>
      </el-header>
      <el-main style="background:#f0f2f0">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
