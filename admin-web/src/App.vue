<script setup lang="ts">
// 根组件：仅渲染路由视图，布局由 MainLayout 管理。
</script>

<template>
  <!-- 顶层只渲染当前路由页面，具体布局由 router 决定。 -->
  <router-view />
</template>
