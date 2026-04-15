<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">统一查看系统通知、广播和未读状态。</span>
      </template>
      <template #actions>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="通知中心" description="按未读状态识别需要处理的系统消息。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table" :row-class-name="rowClass">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusBadge :label="row.isRead ? '已读' : '未读'" :tone="row.isRead ? 'success' : 'danger'" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isRead" size="small" @click="markRead(row.id)">标记已读</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="NT" title="暂无通知" description="当前没有新的系统消息。" />
        </template>
      </el-table>
    </TableShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Bell, CircleCheck, MessageBox, Warning } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../api'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])

const metrics = computed(() => [
  { label: '通知总数', value: list.value.length, helper: '当前通知中心条目', icon: Bell, tone: 'accent' },
  { label: '未读消息', value: list.value.filter(item => !item.isRead).length, helper: '待处理消息数量', icon: Warning, tone: 'danger' },
  { label: '已读消息', value: list.value.filter(item => item.isRead).length, helper: '已阅读数量', icon: CircleCheck, tone: 'success' },
  { label: '消息类型', value: new Set(list.value.map(item => item.type).filter(Boolean)).size, helper: '当前类型种数', icon: MessageBox, tone: 'neutral' }
])

const rowClass = ({ row }) => (row.isRead ? '' : 'unread-row')

const load = async () => {
  list.value = await apiGet('/api/notification')
}

const markRead = async (id) => {
  await apiPost(`/api/notification/${id}/read`)
  await load()
}

onMounted(load)
</script>

<style scoped>
:deep(.unread-row) {
  background: rgba(237, 244, 255, 0.82) !important;
}
</style>
