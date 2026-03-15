<template>
  <div class="page-card">
    <h2 class="page-title">消息通知</h2>
    <el-space style="margin-bottom:12px">
      <el-button @click="load">刷新</el-button>
      <el-tag>未读: {{ unreadCount }}</el-tag>
    </el-space>

    <el-table :data="list" border :row-class-name="rowClass">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="isRead" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.isRead ? 'success' : 'danger'" size="small">
            {{ scope.row.isRead ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button size="small" v-if="!scope.row.isRead" @click="markRead(scope.row.id)">标记已读</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { apiGet, apiPost } from '../../api'

const list = ref([])
const unreadCount = computed(() => list.value.filter(n => !n.isRead).length)

const rowClass = ({ row }) => row.isRead ? '' : 'unread-row'
const load = async () => { list.value = await apiGet('/api/notification') }
const markRead = async (id) => { await apiPost(`/api/notification/${id}/read`); await load() }

onMounted(load)
</script>

<style scoped>
:deep(.unread-row) {
  background-color: #f0f6ff !important;
  font-weight: 600;
}
</style>
