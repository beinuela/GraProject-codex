<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <el-select v-model="warehouseFilter" clearable placeholder="按仓库筛选" style="width: 220px" @change="handleWarehouseFilterChange">
          <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.warehouseName" :value="warehouse.id" />
        </el-select>
      </template>
      <template #actions>
        <el-button @click="load">刷新</el-button>
        <el-button @click="loadBatches">查看批次</el-button>
        <el-button type="primary" @click="openBatchCreate">新增批次</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="库存台账" description="查看当前库存、锁定量和安全库存阈值。" :badge="`${pagination.total} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="materialId" label="物资ID" width="100" />
        <el-table-column prop="materialName" label="物资名称" min-width="180" />
        <el-table-column prop="warehouseId" label="仓库ID" width="100" />
        <el-table-column prop="currentQty" label="当前库存" width="110" />
        <el-table-column prop="lockedQty" label="锁定数量" width="110" />
        <el-table-column prop="safetyStock" label="安全库存" width="110" />
        <template #empty>
          <EmptyState glyph="IV" title="暂无库存记录" description="当前筛选条件下没有库存台账。" />
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

    <DialogShell v-model="batchVisible" title="批次明细" eyebrow="Batch Detail" subtitle="查看同一筛选条件下的库存批次与效期信息。" width="860">
      <el-form :model="batchForm" label-position="top" class="form-grid form-grid--3">
        <el-form-item label="物资">
          <el-select v-model="batchForm.materialId" filterable placeholder="选择物资">
            <el-option v-for="material in materials" :key="material.id" :label="material.materialName" :value="material.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="batchForm.warehouseId" filterable placeholder="选择仓库">
            <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.warehouseName" :value="warehouse.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="batchForm.batchNo" />
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="batchForm.inQty" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="剩余数量">
          <el-input-number v-model="batchForm.remainQty" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生产日期">
          <el-date-picker v-model="batchForm.productionDate" value-format="YYYY-MM-DD" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="过期日期">
          <el-date-picker v-model="batchForm.expireDate" value-format="YYYY-MM-DD" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item class="form-actions">
          <el-button @click="resetBatchForm()">清空</el-button>
          <el-button type="primary" @click="saveBatch">{{ batchForm.id ? '保存批次' : '新增批次' }}</el-button>
        </el-form-item>
      </el-form>
      <el-divider />
      <el-table :data="batches" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="batchNo" label="批次号" min-width="160" />
        <el-table-column prop="materialId" label="物资ID" width="100" />
        <el-table-column prop="warehouseId" label="仓库ID" width="100" />
        <el-table-column prop="inQty" label="入库数量" width="110" />
        <el-table-column prop="remainQty" label="剩余数量" width="110" />
        <el-table-column prop="productionDate" label="生产日期" width="140" />
        <el-table-column prop="expireDate" label="过期日期" width="140" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openBatchEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="BT" title="暂无批次明细" description="当前仓库下没有可展示的批次信息。" />
        </template>
      </el-table>
      <template #footer>
        <el-button @click="batchVisible = false">关闭</el-button>
      </template>
    </DialogShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Coin, Lock, Warning } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PaginationBar from '../../components/ui/PaginationBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const batches = ref([])
const warehouses = ref([])
const materials = ref([])
const warehouseFilter = ref(null)
const batchVisible = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })
const batchForm = reactive({
  id: null,
  materialId: null,
  warehouseId: null,
  batchNo: '',
  inQty: 0,
  remainQty: 0,
  productionDate: '',
  expireDate: ''
})

const metrics = computed(() => [
  { label: '库存记录', value: pagination.total, helper: '当前筛选条件下总量', icon: Box, tone: 'accent' },
  { label: '当前库存量', value: list.value.reduce((sum, item) => sum + Number(item.currentQty || 0), 0), helper: '所有记录库存合计', icon: Coin, tone: 'teal' },
  { label: '锁定数量', value: list.value.reduce((sum, item) => sum + Number(item.lockedQty || 0), 0), helper: '待处理占用量', icon: Lock, tone: 'warning' },
  { label: '低于安全库存', value: list.value.filter(item => Number(item.currentQty || 0) < Number(item.safetyStock || 0)).length, helper: '当前页需补货关注', icon: Warning, tone: 'danger' }
])

const loadWarehouses = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
}

const loadMaterials = async () => {
  materials.value = await apiGet('/api/material/info')
}

const load = async () => {
  const result = await apiGet('/api/inventory/list', {
    page: pagination.page,
    size: pagination.size,
    warehouseId: warehouseFilter.value || undefined
  })
  list.value = result.records || []
  pagination.total = Number(result.total || 0)
}

const loadBatches = async (showDialog = true) => {
  batches.value = await apiGet('/api/inventory/batches', {
    warehouseId: warehouseFilter.value || undefined
  })
  if (showDialog) {
    batchVisible.value = true
  }
}

const resetBatchForm = (row = {}) => {
  Object.assign(batchForm, {
    id: row.id || null,
    materialId: row.materialId || null,
    warehouseId: row.warehouseId || warehouseFilter.value || null,
    batchNo: row.batchNo || '',
    inQty: Number(row.inQty || 0),
    remainQty: Number(row.remainQty || 0),
    productionDate: row.productionDate || '',
    expireDate: row.expireDate || ''
  })
}

const openBatchCreate = async () => {
  resetBatchForm()
  await loadBatches()
}

const openBatchEdit = (row) => {
  resetBatchForm(row)
}

const saveBatch = async () => {
  await apiPost('/api/inventory/batches', batchForm)
  ElMessage.success(batchForm.id ? '批次已更新' : '批次已新增')
  resetBatchForm()
  await loadBatches(false)
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

const handleWarehouseFilterChange = async () => {
  pagination.page = 1
  await load()
}

onMounted(async () => {
  await loadWarehouses()
  await loadMaterials()
  await load()
})
</script>
