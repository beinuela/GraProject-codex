<template>
  <PageScaffold :metrics="metrics" page-type="workflow">
    <FilterActionBar>
      <template #filters>
        <el-select v-model="filterStatus" clearable placeholder="状态筛选" style="width: 180px" @change="load">
          <el-option value="OPEN" label="待处理" />
          <el-option value="IN_PROGRESS" label="处理中" />
          <el-option value="CLOSED" label="已关闭" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">上报事件</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="事件列表" description="跟踪事件等级、状态和处理动作。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="eventTitle" label="事件标题" min-width="200" />
        <el-table-column prop="eventType" label="事件类型" width="140" />
        <el-table-column prop="eventLevel" label="等级" width="90" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <StatusBadge :label="statusLabel(row.status)" :tone="statusTone(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="eventTime" label="事件时间" width="180" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button v-if="canHandleEvent && row.status === 'OPEN'" size="small" type="primary" @click="handleEvent(row.id)">处理</el-button>
              <el-button v-if="canCloseEvent && row.status === 'IN_PROGRESS'" size="small" type="success" @click="closeEvent(row.id)">关闭</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="EV" title="暂无事件" description="上报事件后，可在此持续跟踪处理闭环。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="createVisible" title="上报事件" eyebrow="Event Report" subtitle="填写事件标题、类型、等级、描述与发生地点。" width="720">
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="事件标题" style="grid-column: 1 / -1;">
          <el-input v-model="form.eventTitle" />
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="form.eventType">
            <el-option value="NATURAL_DISASTER" label="自然灾害" />
            <el-option value="ACCIDENT" label="事故灾难" />
            <el-option value="PUBLIC_HEALTH" label="公共卫生" />
            <el-option value="SOCIAL_SECURITY" label="社会安全" />
            <el-option value="OTHER" label="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件等级">
          <el-select v-model="form.eventLevel">
            <el-option value="1" label="一级（特别重大）" />
            <el-option value="2" label="二级（重大）" />
            <el-option value="3" label="三级（较大）" />
            <el-option value="4" label="四级（一般）" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件描述" style="grid-column: 1 / -1;">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="发生地点" style="grid-column: 1 / -1;">
          <el-input v-model="form.location" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEvent">提交</el-button>
      </template>
    </DialogShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Flag, SwitchButton, Warning } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'
import { hasAnyRole, roleGroups } from '../../roles'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()
const list = ref([])
const filterStatus = ref('')
const createVisible = ref(false)
const form = reactive({ eventTitle: '', eventType: 'OTHER', eventLevel: '4', description: '', location: '' })
const currentRoleCode = computed(() => authStore.user?.roleCode || localStorage.getItem('roleCode') || '')
const canHandleEvent = computed(() => hasAnyRole(currentRoleCode.value, roleGroups.warehouse))
const canCloseEvent = computed(() => hasAnyRole(currentRoleCode.value, ['ADMIN', 'WAREHOUSE_ADMIN', 'APPROVER']))

const currentLocalDateTime = () => new Date().toISOString().slice(0, 19)

const statusLabel = status => ({ OPEN: '待处理', IN_PROGRESS: '处理中', CLOSED: '已关闭' }[status] || status)
const statusTone = status => ({ OPEN: 'danger', IN_PROGRESS: 'warning', CLOSED: 'success' }[status] || 'neutral')

const metrics = computed(() => [
  { label: '事件总数', value: list.value.length, helper: '当前筛选范围内事件', icon: Flag, tone: 'accent' },
  { label: '待处理', value: list.value.filter(item => item.status === 'OPEN').length, helper: '尚未介入处理', icon: Warning, tone: 'danger' },
  { label: '处理中', value: list.value.filter(item => item.status === 'IN_PROGRESS').length, helper: '正在推进处置', icon: SwitchButton, tone: 'warning' },
  { label: '已关闭', value: list.value.filter(item => item.status === 'CLOSED').length, helper: '已完成事件闭环', icon: Flag, tone: 'success' }
])

const load = async () => {
  const params = new URLSearchParams()
  if (filterStatus.value) params.append('status', filterStatus.value)
  const suffix = params.toString() ? `?${params}` : ''
  list.value = await apiGet(`/api/event${suffix}`)
}

const openCreate = () => {
  Object.assign(form, { eventTitle: '', eventType: 'OTHER', eventLevel: '4', description: '', location: '' })
  createVisible.value = true
}

const saveEvent = async () => {
  await apiPost('/api/event', { ...form, eventTime: currentLocalDateTime() })
  ElMessage.success('事件上报成功')
  createVisible.value = false
  await load()
}

const handleEvent = async (id) => {
  await apiPost(`/api/event/${id}/handle`)
  ElMessage.success('已开始处理')
  await load()
}

const closeEvent = async (id) => {
  const { value } = await ElMessageBox.prompt('请输入处理结果', '关闭事件', { confirmButtonText: '确定', cancelButtonText: '取消' })
  await apiPost(`/api/event/${id}/close`, { handleResult: value })
  ElMessage.success('事件已关闭')
  await load()
}

onMounted(load)
</script>
