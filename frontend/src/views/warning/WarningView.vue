<template>
  <PageScaffold :metrics="metrics" page-type="monitor">
    <FilterActionBar>
      <template #filters>
        <el-select v-model="filterType" clearable placeholder="预警类型" style="width: 180px" @change="applyFilters">
          <el-option value="STOCK_LOW" label="库存不足" />
          <el-option value="STOCK_BACKLOG" label="库存积压" />
          <el-option value="EXPIRING_SOON" label="即将过期" />
          <el-option value="EXPIRED" label="已过期" />
          <el-option value="ABNORMAL_USAGE" label="异常消耗" />
        </el-select>
        <el-select v-model="filterStatus" clearable placeholder="处理状态" style="width: 160px" @change="applyFilters">
          <el-option value="UNHANDLED" label="未处理" />
          <el-option value="HANDLED" label="已处理" />
        </el-select>
      </template>
      <template #actions>
        <el-button @click="load">刷新</el-button>
        <el-button type="warning" @click="triggerScan">手动扫描</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="预警中心" description="查看预警类型、状态和处置动作。" :badge="`${pagination.total} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="预警类型" width="140">
          <template #default="{ row }">
            <StatusBadge :label="typeLabel(row.warningType)" :tone="typeTone(row.warningType)" />
          </template>
        </el-table-column>
        <el-table-column prop="materialId" label="物资ID" width="100" />
        <el-table-column prop="warehouseId" label="仓库ID" width="100" />
        <el-table-column prop="content" label="预警内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <StatusBadge :label="row.handleStatus === 'HANDLED' ? '已处理' : '未处理'" :tone="row.handleStatus === 'HANDLED' ? 'success' : 'danger'" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.handleStatus === 'UNHANDLED'" size="small" type="primary" @click="handleWarning(row.id)">处理</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="WR" title="暂无预警记录" description="当前筛选范围内没有预警项。" />
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
import { ElMessageBox } from 'element-plus'
import { Bell, DataAnalysis, Warning } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../api'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PaginationBar from '../../components/ui/PaginationBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const filterType = ref('')
const filterStatus = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })

const typeLabel = type => ({ STOCK_LOW: '库存不足', STOCK_BACKLOG: '库存积压', EXPIRING_SOON: '即将过期', EXPIRED: '已过期', ABNORMAL_USAGE: '异常消耗' }[type] || type)
const typeTone = type => ({ STOCK_LOW: 'danger', STOCK_BACKLOG: 'warning', EXPIRING_SOON: 'warning', EXPIRED: 'danger', ABNORMAL_USAGE: 'accent' }[type] || 'neutral')

const metrics = computed(() => [
  { label: '预警总数', value: pagination.total, helper: '当前筛选范围内预警总量', icon: Bell, tone: 'accent' },
  { label: '未处理', value: list.value.filter(item => item.handleStatus === 'UNHANDLED').length, helper: '待人工处置', icon: Warning, tone: 'danger' },
  { label: '已处理', value: list.value.filter(item => item.handleStatus === 'HANDLED').length, helper: '当前页已完成闭环', icon: DataAnalysis, tone: 'success' }
])

const load = async () => {
  const result = await apiGet('/api/warning/list', {
    page: pagination.page,
    size: pagination.size,
    type: filterType.value || undefined,
    status: filterStatus.value || undefined
  })
  list.value = result.records || []
  pagination.total = Number(result.total || 0)
}

const triggerScan = async () => {
  await apiPost('/api/warning/scan')
  await load()
}

const handleWarning = async (id) => {
  const { value } = await ElMessageBox.prompt('请输入处理备注', '处理预警', { confirmButtonText: '确定', cancelButtonText: '取消' })
  await apiPost(`/api/warning/${id}/handle`, { remark: value })
  await load()
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

const applyFilters = async () => {
  pagination.page = 1
  await load()
}

onMounted(load)
</script>
