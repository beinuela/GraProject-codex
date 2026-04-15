<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">出库单可以关联申领单，也可以独立记录仓库出库动作。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新建出库单</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="出库记录" description="查看出库单、关联申领和仓库信息。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="applyOrderId" label="申领单ID" width="110" />
        <el-table-column prop="warehouseId" label="仓库ID" width="100" />
        <el-table-column prop="operatorId" label="操作人ID" width="120" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <template #empty>
          <EmptyState glyph="OT" title="暂无出库单" description="创建出库单后，可记录物资数量与申领关联。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" title="新建出库单" eyebrow="Stock Out" subtitle="填写申领单关联、仓库与出库明细。" width="900">
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="申领单ID">
          <el-input-number v-model="form.applyOrderId" :min="0" placeholder="可选" />
        </el-form-item>
        <el-form-item label="出库仓库">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库">
            <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.warehouseName" :value="warehouse.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" style="grid-column: 1 / -1;">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>

      <div class="collection-editor">
        <div class="table-note">出库明细</div>
        <div v-for="(item, index) in form.items" :key="index" class="collection-editor__row collection-editor__row--4">
          <el-select v-model="item.materialId" placeholder="物资">
            <el-option v-for="material in materials" :key="material.id" :label="material.materialName" :value="material.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" />
          <div class="table-note">将沿用后端出库逻辑处理库存扣减。</div>
          <div class="table-note">提交后直接进入现有流程。</div>
          <el-button type="danger" @click="form.items.splice(index, 1)">删除</el-button>
        </div>
        <el-button @click="form.items.push({ materialId: null, quantity: 1 })">添加明细</el-button>
      </div>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">提交出库</el-button>
      </template>
    </DialogShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Document, OfficeBuilding, User } from '@element-plus/icons-vue'
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
const form = reactive({ applyOrderId: null, warehouseId: null, remark: '', items: [{ materialId: null, quantity: 1 }] })

const metrics = computed(() => [
  { label: '出库单数', value: list.value.length, helper: '当前已登记出库单', icon: Document, tone: 'accent' },
  { label: '关联申领', value: list.value.filter(item => item.applyOrderId).length, helper: '已绑定申领单数量', icon: Box, tone: 'teal' },
  { label: '仓库覆盖', value: new Set(list.value.map(item => item.warehouseId).filter(Boolean)).size, helper: '发生出库的仓库', icon: OfficeBuilding, tone: 'neutral' },
  { label: '操作人记录', value: list.value.filter(item => item.operatorId).length, helper: '已记录操作人', icon: User, tone: 'warning' }
])

const loadBase = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
  materials.value = await apiGet('/api/material/info')
}

const load = async () => {
  list.value = await apiGet('/api/inventory/stock-out')
}

const openCreate = () => {
  form.applyOrderId = null
  form.warehouseId = null
  form.remark = ''
  form.items = [{ materialId: null, quantity: 1 }]
  visible.value = true
}

const save = async () => {
  await apiPost('/api/inventory/stock-out', form)
  ElMessage.success('出库成功')
  visible.value = false
  await load()
}

onMounted(async () => {
  await loadBase()
  await load()
})
</script>
