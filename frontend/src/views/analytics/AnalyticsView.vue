<template>
  <div class="analytics-page">
    <h2 class="page-title">统计分析</h2>

    <!-- KPI 概览 -->
    <div class="kpi-row">
      <div class="kpi-card" v-for="(item, idx) in overviewList" :key="item.label" :style="{ animationDelay: idx * 0.06 + 's' }">
        <div class="kpi-icon" :style="{ background: kpiColors[idx] }">
          <el-icon :size="22"><component :is="kpiIcons[idx]" /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-value">{{ item.value }}</div>
          <div class="kpi-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表网格 -->
    <div class="chart-grid">
      <!-- 1. 库存占比饼图 -->
      <div class="chart-card">
        <h3>📦 库存占比分布</h3>
        <div ref="pieRef" class="chart-box"></div>
      </div>
      <!-- 2. 仓库库存柱状图 -->
      <div class="chart-card">
        <h3>🏗️ 仓库库存分布</h3>
        <div ref="warehouseBarRef" class="chart-box"></div>
      </div>
      <!-- 3. 出入库趋势折线图 -->
      <div class="chart-card">
        <h3>📈 出入库趋势 (近6月)</h3>
        <div ref="trendLineRef" class="chart-box"></div>
      </div>
      <!-- 4. 部门领用排行 -->
      <div class="chart-card">
        <h3>🏆 部门领用排行 Top10</h3>
        <div ref="deptBarRef" class="chart-box"></div>
      </div>
      <!-- 5. 物资过期统计 -->
      <div class="chart-card">
        <h3>⏰ 物资过期统计</h3>
        <div ref="expiryBarRef" class="chart-box"></div>
      </div>
      <!-- 6. 应急消耗趋势 -->
      <div class="chart-card">
        <h3>🚨 应急消耗趋势</h3>
        <div ref="emergencyLineRef" class="chart-box"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { apiGet } from '../../api'
import { useChart } from '../../composables/useChart'
import { User, Box, OfficeBuilding, Warning, Bell, Document } from '@element-plus/icons-vue'

const COLORS = ['#3b82f6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6']

// ========== KPI 概览 ==========
const overviewList = ref([])
const kpiIcons = [User, Box, OfficeBuilding, Warning, Document, Bell]
const kpiColors = [
  'linear-gradient(135deg, #3b82f6, #2563eb)',
  'linear-gradient(135deg, #10b981, #059669)',
  'linear-gradient(135deg, #06b6d4, #0891b2)',
  'linear-gradient(135deg, #f59e0b, #d97706)',
  'linear-gradient(135deg, #8b5cf6, #7c3aed)',
  'linear-gradient(135deg, #ef4444, #dc2626)'
]

// ========== 1. 库存占比饼图 ==========
const pieRef = ref(null)
const inventoryData = ref([])
const pieOpt = computed(() => {
  if (!inventoryData.value.length) return null
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { type: 'scroll', bottom: 0, textStyle: { color: '#64748b', fontSize: 12 } },
    color: COLORS,
    series: [{
      type: 'pie', radius: ['38%', '68%'], center: ['50%', '45%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: inventoryData.value.map(d => ({ name: d.name, value: Number(d.value) }))
    }]
  }
})
useChart(pieRef, pieOpt)

// ========== 2. 仓库库存柱状图 ==========
const warehouseBarRef = ref(null)
const warehouseData = ref([])
const warehouseBarOpt = computed(() => {
  if (!warehouseData.value.length) return null
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: warehouseData.value.map(d => d.warehouseName), axisLabel: { color: '#94a3b8', rotate: 20, fontSize: 11 }, axisLine: { lineStyle: { color: '#e2e8f0' } } },
    yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [{
      type: 'bar', data: warehouseData.value.map(d => Number(d.totalQty)),
      barWidth: '50%',
      itemStyle: { borderRadius: [6, 6, 0, 0], color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#06b6d4' }] } }
    }]
  }
})
useChart(warehouseBarRef, warehouseBarOpt)

// ========== 3. 出入库趋势折线图 ==========
const trendLineRef = ref(null)
const trendData = ref([])
const trendLineOpt = computed(() => {
  if (!trendData.value.length) return null
  const months = trendData.value.map(d => d.month_key || d.monthKey)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库', '出库'], textStyle: { color: '#64748b' } },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: months, axisLabel: { color: '#94a3b8' }, axisLine: { lineStyle: { color: '#e2e8f0' } } },
    yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    color: ['#3b82f6', '#ef4444'],
    series: [
      { name: '入库', type: 'line', data: trendData.value.map(d => Number(d.inQty || d.in_qty || 0)), smooth: true, areaStyle: { opacity: 0.12 }, symbolSize: 6 },
      { name: '出库', type: 'line', data: trendData.value.map(d => Number(d.outQty || d.out_qty || 0)), smooth: true, areaStyle: { opacity: 0.12 }, symbolSize: 6 }
    ]
  }
})
useChart(trendLineRef, trendLineOpt)

// ========== 4. 部门领用排行横向柱状图 ==========
const deptBarRef = ref(null)
const deptData = ref([])
const deptBarOpt = computed(() => {
  if (!deptData.value.length) return null
  const sorted = [...deptData.value].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 100, right: 30, top: 10, bottom: 20 },
    xAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    yAxis: { type: 'category', data: sorted.map(d => d.deptName), axisLabel: { color: '#64748b', fontSize: 12 } },
    series: [{
      type: 'bar', data: sorted.map(d => Number(d.totalQty)),
      barWidth: '60%',
      itemStyle: { borderRadius: [0, 6, 6, 0], color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#8b5cf6' }, { offset: 1, color: '#ec4899' }] } }
    }]
  }
})
useChart(deptBarRef, deptBarOpt)

// ========== 5. 物资过期统计堆叠柱状图 ==========
const expiryBarRef = ref(null)
const expiryData = ref([])
const expiryBarOpt = computed(() => {
  if (!expiryData.value.length) return null
  const names = expiryData.value.map(d => d.materialName)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['已过期', '即将过期(30天内)'], textStyle: { color: '#64748b' } },
    grid: { left: 50, right: 20, top: 40, bottom: 50 },
    xAxis: { type: 'category', data: names, axisLabel: { color: '#94a3b8', rotate: 30, fontSize: 11 }, axisLine: { lineStyle: { color: '#e2e8f0' } } },
    yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    color: ['#ef4444', '#f59e0b'],
    series: [
      { name: '已过期', type: 'bar', stack: 'total', data: expiryData.value.map(d => Number(d.expiredQty || 0)), itemStyle: { borderRadius: [0, 0, 0, 0] } },
      { name: '即将过期(30天内)', type: 'bar', stack: 'total', data: expiryData.value.map(d => Number(d.expiringQty || 0)), itemStyle: { borderRadius: [4, 4, 0, 0] } }
    ]
  }
})
useChart(expiryBarRef, expiryBarOpt)

// ========== 6. 应急消耗面积图 ==========
const emergencyLineRef = ref(null)
const emergencyData = ref([])
const emergencyLineOpt = computed(() => {
  if (!emergencyData.value.length) return null
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: emergencyData.value.map(d => d.monthKey || d.month_key), axisLabel: { color: '#94a3b8' }, axisLine: { lineStyle: { color: '#e2e8f0' } } },
    yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [{
      type: 'line', data: emergencyData.value.map(d => Number(d.totalQty || 0)),
      smooth: true, symbolSize: 6,
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(239,68,68,0.35)' }, { offset: 1, color: 'rgba(239,68,68,0.02)' }] } },
      lineStyle: { color: '#ef4444', width: 2 },
      itemStyle: { color: '#ef4444' }
    }]
  }
})
useChart(emergencyLineRef, emergencyLineOpt)

// ========== 数据加载 ==========
const load = async () => {
  try {
    const ov = await apiGet('/api/analytics/overview')
    overviewList.value = [
      { label: '系统用户', value: ov.userCount ?? 0 },
      { label: '物资种类', value: ov.materialCount ?? 0 },
      { label: '仓库数量', value: ov.warehouseCount ?? 0 },
      { label: '未处理预警', value: ov.unhandledWarningCount ?? 0 },
      { label: '待审批申领', value: ov.pendingApplyCount ?? 0 },
      { label: '待审批调拨', value: ov.pendingTransferCount ?? 0 }
    ]
  } catch { overviewList.value = [] }

  try { inventoryData.value = await apiGet('/api/analytics/inventory-ratio') } catch { inventoryData.value = [] }
  try { warehouseData.value = await apiGet('/api/analytics/warehouse-distribution') } catch { warehouseData.value = [] }
  try { trendData.value = await apiGet('/api/analytics/inbound-outbound-trend') } catch { trendData.value = [] }
  try { deptData.value = await apiGet('/api/analytics/department-ranking') } catch { deptData.value = [] }
  try { expiryData.value = await apiGet('/api/analytics/expiry-stats') } catch { expiryData.value = [] }
  try { emergencyData.value = await apiGet('/api/analytics/emergency-consumption') } catch { emergencyData.value = [] }
}

onMounted(load)
</script>

<style scoped>
.analytics-page { padding-bottom: 24px; }

.kpi-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}
.kpi-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-card, #fff);
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
  animation: fadeInUp 0.4s cubic-bezier(0.25, 1, 0.5, 1) both;
}
.kpi-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}
.kpi-info { flex: 1; min-width: 0; }
.kpi-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary, #1e293b);
  line-height: 1.2;
}
.kpi-label {
  margin-top: 2px;
  color: var(--text-regular, #64748b);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.chart-card {
  background: var(--bg-card, #fff);
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
}
.chart-card h3 {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}
.chart-box {
  width: 100%;
  height: 300px;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1200px) {
  .kpi-row { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .chart-grid { grid-template-columns: 1fr; }
}
</style>
