<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">查看登录用户、IP、状态与时间记录。</span>
      </template>
      <template #actions>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="登录日志" description="记录用户登录成功与失败的轨迹。" :badge="`${pagination.total} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="loginIp" label="登录 IP" width="160" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusBadge :label="isSuccessStatus(row.loginStatus) ? '成功' : '失败'" :tone="isSuccessStatus(row.loginStatus) ? 'success' : 'danger'" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="登录时间" width="180" />
        <template #empty>
          <EmptyState glyph="LG" title="暂无登录日志" description="登录日志为空，说明近期还没有记录被拉取。" />
        </template>
      </el-table>
    </TableShell>
    <PaginationBar
      :page="pagination.page"
      :size="pagination.size"
      :total="pagination.total"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, CloseBold, User } from '@element-plus/icons-vue'
import { apiGet } from '../../api'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PaginationBar from '../../components/ui/PaginationBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const isSuccessStatus = value => value === '1' || value === 'SUCCESS'

const metrics = computed(() => [
  { label: '日志总数', value: pagination.total, helper: '当前已拉取登录记录总量', icon: User, tone: 'accent' },
  { label: '登录成功', value: list.value.filter(item => isSuccessStatus(item.loginStatus)).length, helper: '当前页成功状态记录数', icon: Check, tone: 'success' },
  { label: '登录失败', value: list.value.filter(item => !isSuccessStatus(item.loginStatus)).length, helper: '当前页失败状态记录数', icon: CloseBold, tone: 'danger' },
  { label: '涉及用户', value: new Set(list.value.map(item => item.username).filter(Boolean)).size, helper: '出现过的用户名数量', icon: User, tone: 'neutral' }
])

const load = async () => {
  const result = await apiGet('/api/login-log', {
    page: pagination.page,
    size: pagination.size
  })
  list.value = result.records || []
  pagination.total = Number(result.total || 0)
}

const handlePageChange = async (page) => {
  pagination.page = page
  await load()
}

const handleSizeChange = async (size) => {
  pagination.size = size
  pagination.page = 1
  await load()
}

onMounted(load)
</script>
