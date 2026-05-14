<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const username = ref('')
const password = ref('')

function handleLogin() {
  auth.login(username.value, password.value).catch(() => {})
}
</script>

<template>
  <div style="display:flex;align-items:center;justify-content:center;min-height:100vh;background:linear-gradient(135deg,#00573D 0%,#2E7D32 100%)">
    <el-card style="width:400px;border-radius:12px">
      <h2 style="text-align:center;margin-bottom:24px;color:#00573D">拾归管理后台</h2>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%;background:#00573D;border-color:#00573D" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
