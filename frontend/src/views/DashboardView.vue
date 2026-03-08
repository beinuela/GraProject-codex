<template>
  <div>
    <h2 class="page-title">首页仪表盘</h2>
    <div class="cards">
      <el-card v-for="item in cardItems" :key="item.key" class="kpi">
        <div class="kpi-title">{{ item.label }}</div>
        <div class="kpi-value">{{ overview[item.key] || 0 }}</div>
      </el-card>
    </div>

    <div class="chart-grid">
      <el-card class="page-card">
        <h3>库存占比</h3>
        <div ref="ratioRef" class="chart"></div>
      </el-card>
      <el-card class="page-card">
        <h3>月度出入库趋势</h3>
        <div ref="trendRef" class="chart"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { apiGet } from '../api'

const ratioRef = ref()
const trendRef = ref()
const overview = reactive({})

const cardItems = [
  { key: 'userCount', label: '系统用户' },
  { key: 'materialCount', label: '物资种类' },
  { key: 'warehouseCount', label: '仓库数量' },
  { key: 'unhandledWarningCount', label: '待处理预警' },
  { key: 'pendingApplyCount', label: '待审申请' },
  { key: 'pendingTransferCount', label: '待审调拨' }
]

const load = async () => {
  Object.assign(overview, await apiGet('/api/analytics/overview'))

  const ratio = await apiGet('/api/analytics/inventory-ratio')
  const ratioChart = echarts.init(ratioRef.value)
  ratioChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['35%', '65%'], data: ratio }]
  })

  const trend = await apiGet('/api/analytics/inbound-outbound-trend')
  const trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map(i => i.month_key || i.monthKey) },
    yAxis: { type: 'value' },
    series: [
      { name: '入库', type: 'bar', data: trend.map(i => i.inQty || i.inqty || 0) },
      { name: '出库', type: 'line', smooth: true, data: trend.map(i => i.outQty || i.outqty || 0) }
    ]
  })
}

onMounted(load)
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.kpi {
  border-radius: 12px;
}

.kpi-title {
  color: #5f6b7a;
}

.kpi-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #0f6b63;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.chart {
  height: 320px;
}

@media (max-width: 980px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
