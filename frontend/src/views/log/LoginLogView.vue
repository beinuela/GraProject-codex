<template>
  <div class="page-card">
    <h2 class="page-title">登录日志</h2>
    <el-space style="margin-bottom:12px">
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="loginIp" label="登录IP" width="140" />
      <el-table-column prop="loginStatus" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.loginStatus === '1' ? 'success' : 'danger'" size="small">
            {{ scope.row.loginStatus === '1' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="登录时间" width="170" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { apiGet } from '../../api'

const list = ref([])

const load = async () => { list.value = await apiGet('/api/login-log') }

onMounted(load)
</script>
