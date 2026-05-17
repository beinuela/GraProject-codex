<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <el-select v-model="statusFilter" clearable placeholder="按状态筛选" style="width: 220px" @change="handleStatusChange">
          <el-option label="待派单" value="PENDING" />
          <el-option label="已派单" value="ASSIGNED" />
          <el-option label="配送中" value="IN_TRANSIT" />
          <el-option label="已签收" value="SIGNED" />
        </el-select>
      </template>
      <template #actions>
        <el-button @click="load">刷新</el-button>
        <el-button v-if="canManage" type="primary" @click="openCreate">生成配送任务</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="配送任务" description="跟踪申领或出库后的配送派单、配送中和签收状态。" :badge="`${pagination.total} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="applyOrderId" label="申领单ID" width="110" />
        <el-table-column prop="stockOutId" label="出库单ID" width="110" />
        <el-table-column prop="receiverName" label="收货人" min-width="120" />
        <el-table-column prop="receiverPhone" label="联系电话" min-width="140" />
        <el-table-column prop="deliveryAddress" label="配送地址" min-width="220" />
        <el-table-column prop="dispatcherId" label="配送人员ID" width="120" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <StatusBadge :label="statusText(row.status)" :tone="statusTone(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="signedAt" label="签收时间" min-width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button v-if="canDispatch && canAssign(row)" size="small" @click="assignTask(row)">派单</el-button>
              <el-button v-if="canDispatch && row.status === 'ASSIGNED'" size="small" type="primary" @click="startTask(row)">开始配送</el-button>
              <el-button v-if="canSign(row)" size="small" type="success" @click="signTask(row)">签收</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="DL" title="暂无配送任务" description="出库后可在此生成配送任务并跟踪签收。" />
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

    <DialogShell v-model="visible" title="生成配送任务" eyebrow="Delivery Task" subtitle="关联申领单或出库单，填写收货人与配送地址。" width="720">
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="申领单ID">
          <el-input-number v-model="form.applyOrderId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="出库单ID">
          <el-input-number v-model="form.stockOutId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收货人">
          <el-input v-model="form.receiverName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.receiverPhone" />
        </el-form-item>
        <el-form-item label="配送地址" class="form-grid__wide">
          <el-input v-model="form.deliveryAddress" />
        </el-form-item>
        <el-form-item label="备注" class="form-grid__wide">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </DialogShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Clock, DocumentChecked, Van } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../api'
import { useAuthStore } from '../../store/auth'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import PaginationBar from '../../components/ui/PaginationBar.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'

const auth = useAuthStore()
const list = ref([])
const visible = ref(false)
const statusFilter = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })
const form = reactive({
  applyOrderId: null,
  stockOutId: null,
  receiverName: '',
  receiverPhone: '',
  deliveryAddress: '',
  remark: ''
})

const canManage = computed(() => ['ADMIN', 'WAREHOUSE_ADMIN', 'DISPATCHER'].includes(auth.user?.roleCode))
const canDispatch = computed(() => ['ADMIN', 'DISPATCHER'].includes(auth.user?.roleCode))
const canReceive = computed(() => ['ADMIN', 'DISPATCHER', 'DEPT_USER', 'USER'].includes(auth.user?.roleCode))

const metrics = computed(() => [
  { label: '配送任务', value: pagination.total, helper: '当前筛选条件下总数', icon: Van, tone: 'accent' },
  { label: '待派单', value: list.value.filter(item => item.status === 'PENDING').length, helper: '需要调度处理', icon: Clock, tone: 'warning' },
  { label: '配送中', value: list.value.filter(item => item.status === 'IN_TRANSIT').length, helper: '正在履约的任务', icon: DocumentChecked, tone: 'teal' },
  { label: '已签收', value: list.value.filter(item => item.status === 'SIGNED').length, helper: '当前页完成数量', icon: Check, tone: 'success' }
])

const load = async () => {
  const result = await apiGet('/api/delivery/list', {
    page: pagination.page,
    size: pagination.size,
    status: statusFilter.value || undefined
  })
  list.value = result.records || []
  pagination.total = Number(result.total || 0)
}

const resetForm = () => {
  Object.assign(form, {
    applyOrderId: null,
    stockOutId: null,
    receiverName: '',
    receiverPhone: '',
    deliveryAddress: '',
    remark: ''
  })
}

const openCreate = () => {
  resetForm()
  visible.value = true
}

const save = async () => {
  await apiPost('/api/delivery', form)
  ElMessage.success('配送任务已生成')
  visible.value = false
  await load()
}

const assignTask = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入配送人员用户ID', '派单', {
      confirmButtonText: '确认派单',
      cancelButtonText: '取消',
      inputValue: row.dispatcherId || auth.user?.id || '',
      inputPattern: /^[1-9]\d*$/,
      inputErrorMessage: '请输入有效用户ID'
    })
    await apiPost(`/api/delivery/${row.id}/assign`, { dispatcherId: Number(value) })
    ElMessage.success('已派单')
    await load()
  } catch (_) {
    // 用户取消时不提示错误。
  }
}

const startTask = async (row) => {
  await apiPost(`/api/delivery/${row.id}/start`, {})
  ElMessage.success('配送状态已更新')
  await load()
}

const signTask = async (row) => {
  await apiPost(`/api/delivery/${row.id}/sign`, {})
  ElMessage.success('签收完成')
  await load()
}

const canAssign = (row) => ['PENDING', 'ASSIGNED'].includes(row.status)
const canSign = (row) => canReceive.value && ['ASSIGNED', 'IN_TRANSIT'].includes(row.status)

const statusText = (status) => ({
  PENDING: '待派单',
  ASSIGNED: '已派单',
  IN_TRANSIT: '配送中',
  SIGNED: '已签收'
}[status] || status || '-')

const statusTone = (status) => ({
  PENDING: 'warning',
  ASSIGNED: 'accent',
  IN_TRANSIT: 'teal',
  SIGNED: 'success'
}[status] || 'neutral')

const handlePageChange = async (page) => {
  pagination.page = page
  await load()
}

const handleSizeChange = async (size) => {
  pagination.size = size
  pagination.page = 1
  await load()
}

const handleStatusChange = async () => {
  pagination.page = 1
  await load()
}

onMounted(load)
</script>
