<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <el-select v-model="warehouseFilter" clearable placeholder="按仓库筛选" style="width: 220px" @change="load">
          <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.warehouseName" :value="warehouse.id" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增库位</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="库位管理" description="关注库位容量、已用容量和启停状态。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="locationCode" label="库位编码" min-width="140" />
        <el-table-column prop="locationName" label="库位名称" min-width="160" />
        <el-table-column prop="warehouseName" label="所属仓库" min-width="160" />
        <el-table-column prop="capacity" label="容量" width="100" />
        <el-table-column prop="usedCapacity" label="已用容量" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusBadge :label="row.status === 1 ? '启用' : '停用'" :tone="row.status === 1 ? 'success' : 'danger'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该库位？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="LC" title="暂无库位" description="先创建库位，便于追踪仓库容量与占用情况。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" :title="form.id ? '编辑库位' : '新增库位'" eyebrow="Location Editor" subtitle="维护库位容量、仓库归属和占用量。" width="620">
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="所属仓库">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库">
            <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.warehouseName" :value="warehouse.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库位编码">
          <el-input v-model="form.locationCode" />
        </el-form-item>
        <el-form-item label="库位名称">
          <el-input v-model="form.locationName" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" :min="0" />
        </el-form-item>
        <el-form-item label="已用容量">
          <el-input-number v-model="form.usedCapacity" :min="0" />
        </el-form-item>
        <el-form-item label="备注" style="grid-column: 1 / -1;">
          <el-input v-model="form.remark" type="textarea" />
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
import { Box, OfficeBuilding, PieChart, SwitchButton } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const warehouses = ref([])
const warehouseFilter = ref(null)
const visible = ref(false)
const form = reactive({ id: null, warehouseId: null, locationCode: '', locationName: '', capacity: 0, usedCapacity: 0, remark: '' })

const metrics = computed(() => [
  { label: '库位总数', value: list.value.length, helper: '当前库位节点', icon: Box, tone: 'accent' },
  { label: '启用库位', value: list.value.filter(item => item.status === 1).length, helper: '处于启用状态', icon: SwitchButton, tone: 'success' },
  { label: '仓库覆盖', value: new Set(list.value.map(item => item.warehouseName).filter(Boolean)).size, helper: '关联仓库数量', icon: OfficeBuilding, tone: 'teal' },
  { label: '平均占用率', value: `${averageUsage.value}%`, helper: '按容量与已用容量估算', icon: PieChart, tone: 'warning' }
])

const averageUsage = computed(() => {
  const rows = list.value.filter(item => Number(item.capacity) > 0)
  if (!rows.length) return 0
  const total = rows.reduce((sum, item) => sum + Number(item.usedCapacity || 0) / Number(item.capacity || 1), 0)
  return Math.round((total / rows.length) * 100)
})

const loadWarehouses = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
}

const load = async () => {
  const params = warehouseFilter.value ? `?warehouseId=${warehouseFilter.value}` : ''
  list.value = await apiGet(`/api/location${params}`)
}

const openCreate = () => {
  Object.assign(form, { id: null, warehouseId: null, locationCode: '', locationName: '', capacity: 0, usedCapacity: 0, remark: '' })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const save = async () => {
  await apiPost('/api/location', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/location/${id}`)
  await load()
}

onMounted(async () => {
  await loadWarehouses()
  await load()
})
</script>
