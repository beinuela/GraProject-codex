<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">登记入库单与批次效期信息，保持原有保存接口。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新建入库单</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="入库记录" description="查看已创建的入库单与来源类型。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="warehouseId" label="仓库ID" width="100" />
        <el-table-column prop="sourceType" label="来源类型" width="140" />
        <el-table-column prop="operatorId" label="操作人ID" width="120" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <template #empty>
          <EmptyState glyph="IN" title="暂无入库单" description="创建入库单后可登记批次、数量和效期。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" title="新建入库单" eyebrow="Stock In" subtitle="录入目标仓库、来源类型与入库明细。" width="980">
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="目标仓库">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库">
            <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.warehouseName" :value="warehouse.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="form.sourceType">
            <el-option value="PURCHASE" label="采购入库" />
            <el-option value="TRANSFER" label="调拨入库" />
            <el-option value="OTHER" label="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" style="grid-column: 1 / -1;">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>

      <div class="collection-editor">
        <div class="table-note">入库明细</div>
        <div v-for="(item, index) in form.items" :key="index" class="collection-editor__row">
          <el-select v-model="item.materialId" placeholder="物资">
            <el-option v-for="material in materials" :key="material.id" :label="material.materialName" :value="material.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" />
          <el-input v-model="item.batchNo" placeholder="批次号" />
          <el-date-picker v-model="item.productionDate" type="date" value-format="YYYY-MM-DD" placeholder="生产日期" />
          <el-date-picker v-model="item.expireDate" type="date" value-format="YYYY-MM-DD" placeholder="过期日期" />
          <el-button type="danger" @click="form.items.splice(index, 1)">删除</el-button>
        </div>
        <el-button @click="form.items.push({ materialId: null, quantity: 1, batchNo: '', productionDate: '', expireDate: '' })">添加明细</el-button>
      </div>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">提交入库</el-button>
      </template>
    </DialogShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Calendar, OfficeBuilding, Tickets } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const warehouses = ref([])
const materials = ref([])
const visible = ref(false)
const form = reactive({ warehouseId: null, sourceType: 'PURCHASE', remark: '', items: [{ materialId: null, quantity: 1, batchNo: '', productionDate: '', expireDate: '' }] })

const metrics = computed(() => [
  { label: '入库单数', value: list.value.length, helper: '当前已登记入库单', icon: Tickets, tone: 'accent' },
  { label: '仓库覆盖', value: new Set(list.value.map(item => item.warehouseId).filter(Boolean)).size, helper: '已发生入库的仓库', icon: OfficeBuilding, tone: 'teal' },
  { label: '采购入库', value: list.value.filter(item => item.sourceType === 'PURCHASE').length, helper: '采购来源数量', icon: Box, tone: 'neutral' },
  { label: '最近记录', value: list.value.filter(item => item.createdAt).length, helper: '已写入时间记录', icon: Calendar, tone: 'warning' }
])

const loadBase = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
  materials.value = await apiGet('/api/material/info')
}

const load = async () => {
  list.value = await apiGet('/api/inventory/stock-in')
}

const openCreate = () => {
  form.warehouseId = null
  form.sourceType = 'PURCHASE'
  form.remark = ''
  form.items = [{ materialId: null, quantity: 1, batchNo: '', productionDate: '', expireDate: '' }]
  visible.value = true
}

const save = async () => {
  await apiPost('/api/inventory/stock-in', form)
  ElMessage.success('入库成功')
  visible.value = false
  await load()
}

onMounted(async () => {
  await loadBase()
  await load()
})
</script>
