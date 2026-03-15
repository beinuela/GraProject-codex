<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>
    <div class="kpi-row">
      <div class="kpi-card" v-for="item in kpiList" :key="item.label">
        <div class="kpi-value">{{ item.value }}</div>
        <div class="kpi-label">{{ item.label }}</div>
      </div>
    </div>
    <div class="ratio-layout">
      <div class="page-card">
        <h3>最近操作日志</h3>
        <el-table :data="recentLogs" border size="small">
          <el-table-column prop="module" label="模块" width="100" />
          <el-table-column prop="action" label="操作" width="120" />
          <el-table-column prop="detail" label="详情" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="170" />
        </el-table>
      </div>
      <div class="page-card">
        <h3>未处理预警</h3>
        <el-table :data="unhandledWarnings" border size="small">
          <el-table-column prop="warningType" label="类型" width="120" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="170" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { apiGet } from '../api'

const kpiList = ref([])
const recentLogs = ref([])
const unhandledWarnings = ref([])

const load = async () => {
  try {
    const inv = await apiGet('/api/inventory/list')
    const warnings = await apiGet('/api/warning/list?status=UNHANDLED')
    const materials = await apiGet('/api/material/info')
    const warehouses = await apiGet('/api/warehouse/list')
    kpiList.value = [
      { label: '物资种类', value: materials.length },
      { label: '仓库数量', value: warehouses.length },
      { label: '库存记录', value: inv.length },
      { label: '未处理预警', value: warnings.length }
    ]
    unhandledWarnings.value = warnings.slice(0, 10)
  } catch {
    kpiList.value = []
  }
  try {
    const logs = await apiGet('/api/log/list')
    recentLogs.value = logs.slice(0, 10)
  } catch {
    recentLogs.value = []
  }
}

onMounted(load)
</script>

<style scoped>
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.kpi-card {
  background: var(--bg-card, #fff);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
}
.kpi-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary, #2563EB);
}
.kpi-label {
  margin-top: 8px;
  color: var(--text-regular, #666);
  font-size: 14px;
}
.ratio-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 640px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .ratio-layout { grid-template-columns: 1fr; }
}
</style>
