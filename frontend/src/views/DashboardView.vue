<template>
  <PageScaffold :metrics="dashboardMetrics" page-type="dashboard">
    <template #hero-actions>
      <el-button @click="load">刷新概览</el-button>
      <el-button type="primary" @click="router.push('/analytics/charts')">进入分析中心</el-button>
    </template>

    <div class="surface-grid surface-grid--2">
      <DataPanel title="库存占比分布" description="按物资类型查看当前库存结构。">
        <div ref="pieChartRef" class="chart-stage"></div>
      </DataPanel>
      <DataPanel title="近六个月出入库趋势" description="追踪入库与出库量的变化节奏。">
        <div ref="lineChartRef" class="chart-stage"></div>
      </DataPanel>
    </div>

    <div class="surface-grid surface-grid--2">
      <TableShell title="最近操作日志" description="最近 10 条关键操作记录。" :badge="`${recentLogs.length} 条`">
        <el-table :data="recentLogs" class="list-table">
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="action" label="操作" width="140" />
          <el-table-column prop="detail" label="详情" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="180" />
          <template #empty>
            <EmptyState glyph="LG" title="暂无操作日志" description="系统尚未产生新的操作记录。" />
          </template>
        </el-table>
      </TableShell>

      <TableShell title="未处理预警" description="需要优先关注的库存和效期风险。" :badge="`${unhandledWarnings.length} 条`">
        <el-table :data="unhandledWarnings" class="list-table">
          <el-table-column prop="warningType" label="类型" width="140">
            <template #default="{ row }">
              <StatusBadge :label="typeLabel(row.warningType)" :tone="typeTone(row.warningType)" />
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="180" />
          <template #empty>
            <EmptyState glyph="WR" title="暂无未处理预警" description="当前没有待跟进的风险事项。" />
          </template>
        </el-table>
      </TableShell>
    </div>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Box, DataAnalysis, OfficeBuilding, Warning } from '@element-plus/icons-vue'
import { apiGet } from '../api'
import { useChart } from '../composables/useChart'
import DataPanel from '../components/ui/DataPanel.vue'
import EmptyState from '../components/ui/EmptyState.vue'
import PageScaffold from '../components/ui/PageScaffold.vue'
import StatusBadge from '../components/ui/StatusBadge.vue'
import TableShell from '../components/ui/TableShell.vue'

const router = useRouter()

const chartColors = ['#2670e9', '#35d4c6', '#7856ff', '#ff9f43', '#fa5252', '#12b886']

const summary = ref({
  materials: 0,
  warehouses: 0,
  inventoryRecords: 0,
  warnings: 0
})
const recentLogs = ref([])
const unhandledWarnings = ref([])
const inventoryRatio = ref([])
const trendData = ref([])

const dashboardMetrics = computed(() => [
  { label: '物资种类', value: summary.value.materials, helper: '已建档物资类型', icon: Box, tone: 'accent' },
  { label: '仓库数量', value: summary.value.warehouses, helper: '参与调度的仓储节点', icon: OfficeBuilding, tone: 'teal' },
  { label: '库存记录', value: summary.value.inventoryRecords, helper: '当前库存台账条数', icon: DataAnalysis, tone: 'neutral' },
  { label: '未处理预警', value: summary.value.warnings, helper: '待跟进风险数量', icon: Warning, tone: summary.value.warnings ? 'danger' : 'warning' }
])

const typeLabel = (type) => ({
  STOCK_LOW: '库存不足',
  STOCK_BACKLOG: '库存积压',
  EXPIRING_SOON: '即将过期',
  EXPIRED: '已过期',
  ABNORMAL_USAGE: '异常消耗'
}[type] || type)

const typeTone = (type) => ({
  STOCK_LOW: 'danger',
  STOCK_BACKLOG: 'warning',
  EXPIRING_SOON: 'warning',
  EXPIRED: 'danger',
  ABNORMAL_USAGE: 'accent'
}[type] || 'neutral')

const pieChartRef = ref(null)
const pieOptions = computed(() => {
  if (!inventoryRatio.value.length) return null
  return {
    tooltip: { trigger: 'item', borderRadius: 14 },
    legend: { type: 'scroll', bottom: 0, textStyle: { color: '#6b7b96' } },
    color: chartColors,
    series: [
      {
        type: 'pie',
        radius: ['42%', '72%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 700 } },
        data: inventoryRatio.value.map(item => ({ name: item.name, value: Number(item.value) }))
      }
    ]
  }
})
useChart(pieChartRef, pieOptions)

const lineChartRef = ref(null)
const lineOptions = computed(() => {
  if (!trendData.value.length) return null
  return {
    tooltip: { trigger: 'axis', borderRadius: 14 },
    legend: { data: ['入库', '出库'], textStyle: { color: '#6b7b96' } },
    grid: { left: 50, right: 20, top: 42, bottom: 34 },
    xAxis: {
      type: 'category',
      data: trendData.value.map(item => item.monthKey || item.month_key),
      axisLine: { lineStyle: { color: '#d8e1ef' } },
      axisLabel: { color: '#73849f' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#73849f' },
      splitLine: { lineStyle: { color: '#edf2fa' } }
    },
    color: ['#2670e9', '#fa5252'],
    series: [
      {
        name: '入库',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        areaStyle: { opacity: 0.12 },
        data: trendData.value.map(item => Number(item.inQty || item.in_qty || 0))
      },
      {
        name: '出库',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        areaStyle: { opacity: 0.12 },
        data: trendData.value.map(item => Number(item.outQty || item.out_qty || 0))
      }
    ]
  }
})
useChart(lineChartRef, lineOptions)

const load = async () => {
  try {
    const [inventory, warnings, materials, warehouses] = await Promise.all([
      apiGet('/api/inventory/list'),
      apiGet('/api/warning/list?status=UNHANDLED'),
      apiGet('/api/material/info'),
      apiGet('/api/warehouse/list')
    ])
    summary.value = {
      materials: materials.length,
      warehouses: warehouses.length,
      inventoryRecords: inventory.length,
      warnings: warnings.length
    }
    unhandledWarnings.value = warnings.slice(0, 10)
  } catch {
    summary.value = { materials: 0, warehouses: 0, inventoryRecords: 0, warnings: 0 }
    unhandledWarnings.value = []
  }

  try {
    const logs = await apiGet('/api/log/list')
    recentLogs.value = logs.slice(0, 10)
  } catch {
    recentLogs.value = []
  }

  try {
    inventoryRatio.value = await apiGet('/api/analytics/inventory-ratio')
  } catch {
    inventoryRatio.value = []
  }

  try {
    trendData.value = await apiGet('/api/analytics/inbound-outbound-trend')
  } catch {
    trendData.value = []
  }
}

onMounted(load)
</script>

<style scoped>
.chart-stage {
  width: 100%;
  height: 320px;
}
</style>
