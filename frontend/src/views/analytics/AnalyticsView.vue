<template>
  <div>
    <h2 class="page-title">统计分析</h2>
    <div class="grid2">
      <div class="page-card">
        <h3>库存分布</h3>
        <el-table :data="inventoryStats" border size="small">
          <el-table-column prop="materialName" label="物资名称" />
          <el-table-column prop="currentQty" label="当前库存" width="120" />
          <el-table-column prop="safetyStock" label="安全库存" width="120" />
        </el-table>
      </div>
      <div class="page-card">
        <h3>预警统计</h3>
        <el-table :data="warningStats" border size="small">
          <el-table-column prop="type" label="预警类型" />
          <el-table-column prop="count" label="数量" width="100" />
        </el-table>
      </div>
      <div class="page-card">
        <h3>近期过期批次</h3>
        <el-table :data="expiry" border size="small">
          <el-table-column prop="batchNo" label="批次号" />
          <el-table-column prop="materialId" label="物资ID" width="90" />
          <el-table-column prop="remainQty" label="剩余量" width="90" />
          <el-table-column prop="expireDate" label="过期日期" width="120" />
        </el-table>
      </div>
      <div class="page-card">
        <h3>物资分类统计</h3>
        <el-table :data="categoryStats" border size="small">
          <el-table-column prop="categoryName" label="分类" />
          <el-table-column prop="count" label="物资数量" width="100" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { apiGet } from '../../api'

const inventoryStats = ref([])
const warningStats = ref([])
const expiry = ref([])
const categoryStats = ref([])

const load = async () => {
  try { inventoryStats.value = await apiGet('/api/analytics/inventory-ratio') } catch { inventoryStats.value = [] }
  try { warningStats.value = await apiGet('/api/analytics/emergency-consumption') } catch { warningStats.value = [] }
  try { expiry.value = await apiGet('/api/analytics/expiry-stats') } catch { expiry.value = [] }
  try {
    const materials = await apiGet('/api/material/info')
    const categories = await apiGet('/api/material/category')
    const map = {}
    categories.forEach(c => { map[c.id] = { categoryName: c.categoryName, count: 0 } })
    materials.forEach(m => { if (map[m.categoryId]) map[m.categoryId].count++ })
    categoryStats.value = Object.values(map)
  } catch { categoryStats.value = [] }
}

onMounted(load)
</script>

<style scoped>
.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 980px) {
  .grid2 {
    grid-template-columns: 1fr;
  }
}
</style>
