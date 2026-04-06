<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>
    <!-- KPI 指标卡片 -->
    <div class="kpi-row">
      <div class="kpi-card" v-for="(item, idx) in kpiList" :key="item.label" :style="{ animationDelay: idx * 0.08 + 's' }">
        <div class="kpi-icon" :style="{ background: kpiColors[idx] }">
          <el-icon :size="24"><component :is="kpiIcons[idx]" /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-value">{{ item.value }}</div>
          <div class="kpi-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表行 -->
    <div class="chart-row">
      <div class="chart-card">
        <h3>库存占比分布</h3>
        <div ref="pieChartRef" class="chart-container"></div>
      </div>
      <div class="chart-card">
        <h3>近6月出入库趋势</h3>
        <div ref="lineChartRef" class="chart-container"></div>
      </div>
    </div>

    <!-- 数据表格行 -->
    <div class="table-row">
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
import { onMounted, ref, computed } from 'vue'
import { apiGet } from '../api'
import { useChart } from '../composables/useChart'
import { Box, Warning, OfficeBuilding, Bell } from '@element-plus/icons-vue'

const COLORS = ['#3b82f6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6']

const kpiList = ref([])
const kpiIcons = [Box, OfficeBuilding, Box, Warning]
const kpiColors = [
  'linear-gradient(135deg, #3b82f6, #2563eb)',
  'linear-gradient(135deg, #06b6d4, #0891b2)',
  'linear-gradient(135deg, #10b981, #059669)',
  'linear-gradient(135deg, #f59e0b, #d97706)'
]
const recentLogs = ref([])
const unhandledWarnings = ref([])

// ======= 饼图 =======
const pieChartRef = ref(null)
const inventoryRatio = ref([])
const pieOptions = computed(() => {
  if (!inventoryRatio.value.length) return null
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { type: 'scroll', bottom: 0, textStyle: { color: '#64748b' } },
    color: COLORS,
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: inventoryRatio.value.map(d => ({ name: d.name, value: Number(d.value) }))
    }]
  }
})
useChart(pieChartRef, pieOptions)

// ======= 折线图 =======
const lineChartRef = ref(null)
const trendData = ref([])
const lineOptions = computed(() => {
  if (!trendData.value.length) return null
  const months = trendData.value.map(d => d.month_key || d.monthKey)
  const inQty = trendData.value.map(d => Number(d.inQty || d.in_qty || 0))
  const outQty = trendData.value.map(d => Number(d.outQty || d.out_qty || 0))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库', '出库'], textStyle: { color: '#64748b' } },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: months, axisLabel: { color: '#64748b' }, axisLine: { lineStyle: { color: '#e2e8f0' } } },
    yAxis: { type: 'value', axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    color: ['#3b82f6', '#ef4444'],
    series: [
      { name: '入库', type: 'line', data: inQty, smooth: true, areaStyle: { opacity: 0.15 }, symbolSize: 6 },
      { name: '出库', type: 'line', data: outQty, smooth: true, areaStyle: { opacity: 0.15 }, symbolSize: 6 }
    ]
  }
})
useChart(lineChartRef, lineOptions)

// ======= 数据加载 =======
const load = async () => {
  try {
    const [inv, warnings, materials, warehouses] = await Promise.all([
      apiGet('/api/inventory/list'),
      apiGet('/api/warning/list?status=UNHANDLED'),
      apiGet('/api/material/info'),
      apiGet('/api/warehouse/list')
    ])
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
  try { const logs = await apiGet('/api/log/list'); recentLogs.value = logs.slice(0, 10) } catch { recentLogs.value = [] }
  try { inventoryRatio.value = await apiGet('/api/analytics/inventory-ratio') } catch { inventoryRatio.value = [] }
  try { trendData.value = await apiGet('/api/analytics/inbound-outbound-trend') } catch { trendData.value = [] }
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
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-card, #fff);
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
  animation: fadeInUp 0.4s cubic-bezier(0.25, 1, 0.5, 1) both;
}
.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}
.kpi-info { flex: 1; }
.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary, #1e293b);
  line-height: 1.2;
}
.kpi-label {
  margin-top: 4px;
  color: var(--text-regular, #64748b);
  font-size: 13px;
}

.chart-row, .table-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}
.chart-card {
  background: var(--bg-card, #fff);
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
}
.chart-card h3 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: var(--text-primary, #1e293b);
}
.chart-container {
  width: 100%;
  height: 320px;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 900px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .chart-row, .table-row { grid-template-columns: 1fr; }
}
</style>
