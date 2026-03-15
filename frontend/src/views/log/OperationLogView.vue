<template>
  <div class="page-card">
    <h2 class="page-title">操作日志</h2>
    <el-space style="margin-bottom:12px">
      <el-input v-model="keyword" placeholder="搜索操作人/模块/操作" style="width:260px" clearable />
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="filteredList" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="operatorId" label="操作人ID" width="100" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="action" label="操作" width="140" />
      <el-table-column prop="detail" label="详情" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="时间" width="170" />
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { apiGet } from '../../api'

const list = ref([])
const keyword = ref('')

const filteredList = computed(() => {
  if (!keyword.value) return list.value
  const kw = keyword.value.toLowerCase()
  return list.value.filter(r =>
    String(r.operatorId).includes(kw) ||
    (r.module && r.module.toLowerCase().includes(kw)) ||
    (r.action && r.action.toLowerCase().includes(kw))
  )
})

const load = async () => { list.value = await apiGet('/api/log/list') }

onMounted(load)
</script>
