<template>
  <div>
    <h2 class="page-title">数据分析与智能决策</h2>

    <div class="page-card" style="margin-bottom: 12px">
      <el-space wrap>
        <el-select v-model="materialId" placeholder="选择物资做预测" style="width:220px">
          <el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" />
        </el-select>
        <el-input-number v-model="months" :min="1" :max="12" />
        <el-button type="primary" @click="loadForecast">需求预测</el-button>
        <el-button @click="loadSuggestions">补货建议</el-button>
      </el-space>
    </div>

    <div class="grid2">
      <el-card class="page-card">
        <h3>部门领用排行</h3>
        <div ref="deptRef" class="chart"></div>
      </el-card>
      <el-card class="page-card">
        <h3>仓库库存分布</h3>
        <div ref="warehouseRef" class="chart"></div>
      </el-card>
      <el-card class="page-card">
        <h3>临期/过期统计</h3>
        <div ref="expiryRef" class="chart"></div>
      </el-card>
      <el-card class="page-card">
        <h3>应急事件消耗趋势</h3>
        <div ref="emergencyRef" class="chart"></div>
      </el-card>
      <el-card class="page-card" style="grid-column: 1 / -1;">
        <h3>移动平均预测</h3>
        <div ref="forecastRef" class="chart"></div>
      </el-card>
    </div>

    <el-card class="page-card" style="margin-top:12px">
      <h3>智能补货建议</h3>
      <el-table :data="suggestions" border>
        <el-table-column prop="materialName" label="物资" />
        <el-table-column prop="currentStock" label="当前库存" width="110" />
        <el-table-column prop="safetyStock" label="安全库存" width="110" />
        <el-table-column prop="avgDailyUsage" label="日均消耗" width="120" />
        <el-table-column prop="targetStock" label="目标库存" width="110" />
        <el-table-column prop="suggestQty" label="建议补货" width="110" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { apiGet } from '../../api'

const materials = ref([])
const materialId = ref(null)
const months = ref(3)
const suggestions = ref([])

const deptRef = ref()
const warehouseRef = ref()
const expiryRef = ref()
const emergencyRef = ref()
const forecastRef = ref()

const renderBar = (el, x, y, label) => {
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: x },
    yAxis: { type: 'value' },
    series: [{ name: label, type: 'bar', data: y, itemStyle: { color: '#0f6b63' } }]
  })
}

const loadBaseCharts = async () => {
  const dept = await apiGet('/api/analytics/department-ranking')
  renderBar(deptRef.value, dept.map(i => i.deptName || i.dept_name), dept.map(i => i.totalQty || 0), '领用数量')

  const warehouse = await apiGet('/api/analytics/warehouse-distribution')
  renderBar(warehouseRef.value, warehouse.map(i => i.warehouseName || i.warehouse_name), warehouse.map(i => i.totalQty || 0), '库存总量')

  const expiry = await apiGet('/api/analytics/expiry-stats')
  const expiryChart = echarts.init(expiryRef.value)
  expiryChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['临期', '过期'] },
    xAxis: { type: 'category', data: expiry.map(i => i.materialName || i.material_name) },
    yAxis: { type: 'value' },
    series: [
      { type: 'bar', name: '临期', data: expiry.map(i => i.expiringQty || 0) },
      { type: 'bar', name: '过期', data: expiry.map(i => i.expiredQty || 0) }
    ]
  })

  const emergency = await apiGet('/api/analytics/emergency-consumption')
  const emChart = echarts.init(emergencyRef.value)
  emChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: emergency.map(i => i.monthKey || i.month_key) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: emergency.map(i => i.totalQty || 0) }]
  })
}

const loadForecast = async () => {
  if (!materialId.value) return
  const data = await apiGet('/api/smart/forecast', { materialId: materialId.value, months: months.value })
  const history = data.history || []
  const forecast = data.forecast || []
  const chart = echarts.init(forecastRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['历史消耗', '预测需求'] },
    xAxis: { type: 'category', data: [...history.map(i => i.monthKey || i.month_key), ...forecast.map(i => i.monthKey || i.month_key)] },
    yAxis: { type: 'value' },
    series: [
      { name: '历史消耗', type: 'line', data: [...history.map(i => i.qty || 0), ...new Array(forecast.length).fill(null)] },
      { name: '预测需求', type: 'line', data: [...new Array(history.length).fill(null), ...forecast.map(i => i.predictQty || 0)] }
    ]
  })
}

const loadSuggestions = async () => {
  suggestions.value = await apiGet('/api/smart/replenishment-suggestions', { guaranteeDays: 7 })
}

onMounted(async () => {
  materials.value = await apiGet('/api/material/info')
  materialId.value = materials.value[0]?.id
  await nextTick()
  await loadBaseCharts()
  await loadForecast()
  await loadSuggestions()
})
</script>

<style scoped>
.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.chart {
  height: 290px;
}

@media (max-width: 980px) {
  .grid2 {
    grid-template-columns: 1fr;
  }
}
</style>
