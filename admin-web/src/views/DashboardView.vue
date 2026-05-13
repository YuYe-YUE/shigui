<template>
  <div>
    <h2 style="margin-bottom:16px">仪表盘</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover">
          <div style="font-size:14px;color:#999">{{ card.title }}</div>
          <div style="font-size:28px;font-weight:700;margin-top:8px">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
// S1 先放占位数据，等后续统计接口完成后再替换为真实请求。
const cards = [
  { title: '注册用户', value: '--' },
  { title: '匹配中单据', value: '--' },
  { title: '今日发布', value: '--' },
  { title: '成功认领', value: '--' },
]
</script>
