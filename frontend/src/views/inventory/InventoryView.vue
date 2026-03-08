<template>
  <div class="page-card">
    <h2 class="page-title">库存查询与盘点</h2>
    <el-space wrap style="margin-bottom:12px">
      <el-select v-model="filters.materialId" clearable placeholder="物资" style="width:180px"><el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" /></el-select>
      <el-select v-model="filters.warehouseId" clearable placeholder="仓库" style="width:180px"><el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" /></el-select>
      <el-button @click="load">查询</el-button>
      <el-button @click="loadBatches">刷新批次</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="库存ID" width="90" />
      <el-table-column prop="materialName" label="物资" />
      <el-table-column prop="warehouseId" label="仓库ID" width="90" />
      <el-table-column prop="currentQty" label="当前库存" width="100" />
      <el-table-column prop="lockedQty" label="锁定库存" width="100" />
      <el-table-column prop="safetyStock" label="安全库存" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="openCheck(scope.row)">盘点调整</el-button>
        </template>
      </el-table-column>
    </el-table>

    <h3 style="margin-top:16px">批次明细（FEFO顺序）</h3>
    <el-table :data="batches" border>
      <el-table-column prop="materialId" label="物资ID" width="90" />
      <el-table-column prop="warehouseId" label="仓库ID" width="90" />
      <el-table-column prop="batchNo" label="批次号" />
      <el-table-column prop="remainQty" label="剩余" width="80" />
      <el-table-column prop="productionDate" label="生产日期" width="120" />
      <el-table-column prop="expireDate" label="过期日期" width="120" />
    </el-table>

    <el-dialog v-model="visible" title="盘点调整" width="460">
      <el-form :model="checkForm" label-width="100px">
        <el-form-item label="库存ID"><el-input v-model="checkForm.inventoryId" disabled /></el-form-item>
        <el-form-item label="实际库存"><el-input-number v-model="checkForm.actualQty" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="checkForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="doCheck">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { apiGet, apiPost } from '../../api'

const materials = ref([])
const warehouses = ref([])
const list = ref([])
const batches = ref([])
const visible = ref(false)
const filters = reactive({ materialId: null, warehouseId: null })
const checkForm = reactive({ inventoryId: null, actualQty: 0, remark: '' })

const loadBase = async () => {
  materials.value = await apiGet('/api/material/info')
  warehouses.value = await apiGet('/api/warehouse/list')
}
const load = async () => {
  list.value = await apiGet('/api/inventory/list', filters)
}
const loadBatches = async () => {
  batches.value = await apiGet('/api/inventory/batches', filters)
}
const openCheck = (row) => {
  Object.assign(checkForm, { inventoryId: row.id, actualQty: row.currentQty, remark: '' })
  visible.value = true
}
const doCheck = async () => {
  await apiPost('/api/inventory/check', checkForm)
  visible.value = false
  await load()
}

onMounted(async () => { await loadBase(); await load(); await loadBatches() })
</script>
