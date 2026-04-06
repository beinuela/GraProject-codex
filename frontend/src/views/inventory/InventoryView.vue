<template>
  <div class="page-card">
    <h2 class="page-title">库存查询</h2>
    <el-space style="margin-bottom:12px">
      <el-select v-model="warehouseFilter" placeholder="按仓库筛选" clearable style="width:180px" @change="load">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <el-button type="warning" @click="loadBatches">查看批次</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="materialId" label="物资ID" width="90" />
      <el-table-column prop="materialName" label="物资名称" />
      <el-table-column prop="warehouseId" label="仓库ID" width="90" />
      <el-table-column prop="currentQty" label="当前库存" width="100" />
      <el-table-column prop="lockedQty" label="锁定数量" width="100" />
      <el-table-column prop="safetyStock" label="安全库存" width="100" />
    </el-table>

    <el-dialog append-to-body v-model="batchVisible" title="批次明细" width="700">
      <el-table :data="batches" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="batchNo" label="批次号" />
        <el-table-column prop="materialId" label="物资ID" width="90" />
        <el-table-column prop="inQty" label="入库数量" width="100" />
        <el-table-column prop="remainQty" label="剩余数量" width="100" />
        <el-table-column prop="productionDate" label="生产日期" width="120" />
        <el-table-column prop="expireDate" label="过期日期" width="120" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { apiGet } from '../../api'

const list = ref([])
const batches = ref([])
const warehouses = ref([])
const warehouseFilter = ref(null)
const batchVisible = ref(false)

const loadWarehouses = async () => { warehouses.value = await apiGet('/api/warehouse/list') }
const load = async () => {
  const params = warehouseFilter.value ? `?warehouseId=${warehouseFilter.value}` : ''
  list.value = await apiGet(`/api/inventory/list${params}`)
}
const loadBatches = async () => {
  const params = warehouseFilter.value ? `?warehouseId=${warehouseFilter.value}` : ''
  batches.value = await apiGet(`/api/inventory/batches${params}`)
  batchVisible.value = true
}

onMounted(async () => { await loadWarehouses(); await load() })
</script>
