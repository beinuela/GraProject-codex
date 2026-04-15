<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <el-input v-model="keyword" clearable placeholder="搜索操作人 / 模块 / 操作" style="width: 280px" />
      </template>
      <template #actions>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="操作日志" description="按模块、操作和操作人检索后台行为。" :badge="`${filteredList.length} 条`">
      <el-table :data="filteredList" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="operatorId" label="操作人ID" width="120" />
        <el-table-column prop="module" label="模块" width="140" />
        <el-table-column prop="action" label="操作" width="160" />
        <el-table-column prop="detail" label="详情" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <template #empty>
          <EmptyState glyph="OP" title="暂无操作日志" description="系统尚未产生新的后台操作记录。" />
        </template>
      </el-table>
    </TableShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { DataLine, Search, Tickets } from '@element-plus/icons-vue'
import { apiGet } from '../../api'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const keyword = ref('')

const filteredList = computed(() => {
  if (!keyword.value) return list.value
  const kw = keyword.value.toLowerCase()
  return list.value.filter(item =>
    String(item.operatorId).includes(kw) ||
    (item.module && item.module.toLowerCase().includes(kw)) ||
    (item.action && item.action.toLowerCase().includes(kw))
  )
})

const metrics = computed(() => [
  { label: '日志总数', value: list.value.length, helper: '当前日志记录总量', icon: Tickets, tone: 'accent' },
  { label: '筛选命中', value: filteredList.value.length, helper: '当前搜索结果', icon: Search, tone: 'neutral' },
  { label: '模块数量', value: new Set(list.value.map(item => item.module).filter(Boolean)).size, helper: '涉及模块类型', icon: DataLine, tone: 'teal' }
])

const load = async () => {
  list.value = await apiGet('/api/log/list')
}

onMounted(load)
</script>
