<template>
  <PageScaffold :metrics="overviewMetrics" page-type="analytics">
    <template #hero-actions>
      <el-button @click="load">刷新分析</el-button>
      <el-button type="primary" @click="router.push('/bigscreen')">打开大屏</el-button>
    </template>

    <div class="surface-grid surface-grid--2">
      <DataPanel title="库存占比分布" description="观察库存结构和重点物资占比。">
        <div ref="pieRef" class="chart-box"></div>
      </DataPanel>
      <DataPanel title="仓库库存分布" description="对比各仓库承载的库存规模。">
        <div ref="warehouseBarRef" class="chart-box"></div>
      </DataPanel>
      <DataPanel title="出入库趋势" description="跟踪近六个月入库与出库变化。">
        <div ref="trendLineRef" class="chart-box"></div>
      </DataPanel>
      <DataPanel title="部门领用排行" description="识别领用量靠前的部门。">
        <div ref="deptBarRef" class="chart-box"></div>
      </DataPanel>
      <DataPanel title="物资效期风险" description="统计已过期和即将过期物资数量。">
        <div ref="expiryBarRef" class="chart-box"></div>
      </DataPanel>
      <DataPanel title="应急消耗趋势" description="观察异常时期的消耗波动。">
        <div ref="emergencyLineRef" class="chart-box"></div>
      </DataPanel>
    </div>

    <div class="surface-grid surface-grid--2">
      <DataPanel title="补货参考" description="基于安全库存与近 30 天出库量生成补货建议。">
        <template #actions>
          <span v-if="!canUseSmart" class="table-note">当前角色仅查看基础统计</span>
        </template>
        <el-table v-if="canUseSmart && replenishmentRows.length" :data="replenishmentRows.slice(0, 5)" size="small" class="list-table">
          <el-table-column prop="materialName" label="物资" min-width="160" />
          <el-table-column prop="currentStock" label="当前库存" width="110" />
          <el-table-column prop="safetyStock" label="安全库存" width="110" />
          <el-table-column prop="suggestQty" label="建议补货" width="110" />
        </el-table>
        <div v-else class="chart-empty">
          {{ canUseSmart ? '暂无补货建议数据。' : '当前角色未开放补货建议接口。' }}
        </div>
      </DataPanel>

      <DataPanel title="出库趋势参考" description="基于历史出库记录计算移动平均并给出未来月份参考值。">
        <template #actions>
          <el-select
            v-if="canUseSmart && forecastOptions.length"
            v-model="forecastMaterialId"
            placeholder="选择物资"
            style="width: 220px;"
            @change="loadForecast"
          >
            <el-option v-for="item in forecastOptions" :key="item.id" :label="item.materialName" :value="item.id" />
          </el-select>
        </template>

        <div v-if="canUseSmart && hasForecastData" class="smart-forecast">
          <div class="smart-forecast__summary">
            近六个月移动平均出库量：<strong>{{ forecastAverageText }}</strong>
          </div>
          <div class="smart-forecast__grid">
            <div>
              <div class="table-note">历史出库</div>
              <el-table :data="forecastData.history || []" size="small" class="list-table">
                <el-table-column prop="monthKey" label="月份" width="120" />
                <el-table-column prop="qty" label="出库量" width="100" />
              </el-table>
            </div>
            <div>
              <div class="table-note">未来参考</div>
              <el-table :data="forecastData.forecast || []" size="small" class="list-table">
                <el-table-column prop="monthKey" label="月份" width="120" />
                <el-table-column prop="predictQty" label="参考值" width="100" />
              </el-table>
            </div>
          </div>
        </div>
        <div v-else class="chart-empty">
          {{ canUseSmart ? '暂无可展示的出库趋势参考。' : '当前角色未开放预测接口。' }}
        </div>
      </DataPanel>
    </div>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Box, Document, OfficeBuilding, User, Warning } from '@element-plus/icons-vue'
import { apiGet } from '../../api'
import DataPanel from '../../components/ui/DataPanel.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import { useChart } from '../../composables/useChart'
import { useAuthStore } from '../../store/auth'

const router = useRouter()
const authStore = useAuthStore()

const chartColors = ['#2670e9', '#35d4c6', '#7856ff', '#ff9f43', '#fa5252', '#12b886']

const overview = ref({
  userCount: 0,
  materialCount: 0,
  warehouseCount: 0,
  unhandledWarningCount: 0,
  pendingApplyCount: 0,
  pendingTransferCount: 0
})

const inventoryData = ref([])
const warehouseData = ref([])
const trendData = ref([])
const deptData = ref([])
const expiryData = ref([])
const emergencyData = ref([])
const replenishmentRows = ref([])
const forecastOptions = ref([])
const forecastMaterialId = ref(null)
const forecastData = ref({ history: [], movingAverage: 0, forecast: [] })

const canUseSmart = computed(() => ['ADMIN', 'WAREHOUSE_ADMIN', 'APPROVER'].includes(authStore.user?.roleCode || localStorage.getItem('roleCode') || ''))
const hasForecastData = computed(() => (forecastData.value.history || []).length || (forecastData.value.forecast || []).length)
const forecastAverageText = computed(() => Number(forecastData.value.movingAverage || 0).toFixed(2))

const overviewMetrics = computed(() => [
  { label: '系统用户', value: overview.value.userCount, helper: '当前可登录账号', icon: User, tone: 'accent' },
  { label: '物资种类', value: overview.value.materialCount, helper: '物资档案总量', icon: Box, tone: 'teal' },
  { label: '仓库数量', value: overview.value.warehouseCount, helper: '已建仓储节点', icon: OfficeBuilding, tone: 'neutral' },
  { label: '未处理预警', value: overview.value.unhandledWarningCount, helper: '待处置风险', icon: Warning, tone: overview.value.unhandledWarningCount ? 'danger' : 'warning' },
  { label: '待审批申领', value: overview.value.pendingApplyCount, helper: '申领流程待办', icon: Document, tone: 'warning' },
  { label: '待审批调拨', value: overview.value.pendingTransferCount, helper: '调拨流程待办', icon: Bell, tone: 'accent' }
])

const pieRef = ref(null)
const pieOpt = computed(() => {
  if (!inventoryData.value.length) return null
  return {
    tooltip: { trigger: 'item', borderRadius: 14 },
    legend: { type: 'scroll', bottom: 0, textStyle: { color: '#6b7b96' } },
    color: chartColors,
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: { label: { show: true, fontWeight: 700 } },
        data: inventoryData.value.map(item => ({ name: item.name, value: Number(item.value) }))
      }
    ]
  }
})
useChart(pieRef, pieOpt)

const warehouseBarRef = ref(null)
const warehouseBarOpt = computed(() => {
  if (!warehouseData.value.length) return null
  return {
    tooltip: { trigger: 'axis', borderRadius: 14 },
    grid: { left: 56, right: 20, top: 20, bottom: 36 },
    xAxis: {
      type: 'category',
      data: warehouseData.value.map(item => item.warehouseName),
      axisLine: { lineStyle: { color: '#d8e1ef' } },
      axisLabel: { color: '#73849f', rotate: 20 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#73849f' },
      splitLine: { lineStyle: { color: '#edf2fa' } }
    },
    series: [
      {
        type: 'bar',
        barWidth: '52%',
        data: warehouseData.value.map(item => Number(item.totalQty)),
        itemStyle: {
          borderRadius: [10, 10, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#2670e9' },
              { offset: 1, color: '#35d4c6' }
            ]
          }
        }
      }
    ]
  }
})
useChart(warehouseBarRef, warehouseBarOpt)

const trendLineRef = ref(null)
const trendLineOpt = computed(() => {
  if (!trendData.value.length) return null
  return {
    tooltip: { trigger: 'axis', borderRadius: 14 },
    legend: { data: ['入库', '出库'], textStyle: { color: '#6b7b96' } },
    grid: { left: 50, right: 20, top: 42, bottom: 34 },
    xAxis: {
      type: 'category',
      data: trendData.value.map(item => item.month_key || item.monthKey),
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
useChart(trendLineRef, trendLineOpt)

const deptBarRef = ref(null)
const deptBarOpt = computed(() => {
  if (!deptData.value.length) return null
  const rows = [...deptData.value].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, borderRadius: 14 },
    grid: { left: 100, right: 24, top: 14, bottom: 20 },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#73849f' },
      splitLine: { lineStyle: { color: '#edf2fa' } }
    },
    yAxis: {
      type: 'category',
      data: rows.map(item => item.deptName),
      axisLabel: { color: '#5d6f8b' }
    },
    series: [
      {
        type: 'bar',
        barWidth: '58%',
        data: rows.map(item => Number(item.totalQty)),
        itemStyle: {
          borderRadius: [0, 10, 10, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#7856ff' },
              { offset: 1, color: '#2670e9' }
            ]
          }
        }
      }
    ]
  }
})
useChart(deptBarRef, deptBarOpt)

const expiryBarRef = ref(null)
const expiryBarOpt = computed(() => {
  if (!expiryData.value.length) return null
  return {
    tooltip: { trigger: 'axis', borderRadius: 14 },
    legend: { data: ['已过期', '即将过期'], textStyle: { color: '#6b7b96' } },
    grid: { left: 50, right: 20, top: 42, bottom: 50 },
    xAxis: {
      type: 'category',
      data: expiryData.value.map(item => item.materialName),
      axisLine: { lineStyle: { color: '#d8e1ef' } },
      axisLabel: { color: '#73849f', rotate: 24 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#73849f' },
      splitLine: { lineStyle: { color: '#edf2fa' } }
    },
    color: ['#fa5252', '#ff9f43'],
    series: [
      { name: '已过期', type: 'bar', stack: 'total', data: expiryData.value.map(item => Number(item.expiredQty || 0)) },
      { name: '即将过期', type: 'bar', stack: 'total', data: expiryData.value.map(item => Number(item.expiringQty || item.expiringSoonQty || 0)) }
    ]
  }
})
useChart(expiryBarRef, expiryBarOpt)

const emergencyLineRef = ref(null)
const emergencyLineOpt = computed(() => {
  if (!emergencyData.value.length) return null
  return {
    tooltip: { trigger: 'axis', borderRadius: 14 },
    grid: { left: 50, right: 20, top: 20, bottom: 32 },
    xAxis: {
      type: 'category',
      data: emergencyData.value.map(item => item.monthKey || item.month_key),
      axisLine: { lineStyle: { color: '#d8e1ef' } },
      axisLabel: { color: '#73849f' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#73849f' },
      splitLine: { lineStyle: { color: '#edf2fa' } }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 8,
        lineStyle: { color: '#fa5252', width: 2 },
        itemStyle: { color: '#fa5252' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(250, 82, 82, 0.26)' },
              { offset: 1, color: 'rgba(250, 82, 82, 0.02)' }
            ]
          }
        },
        data: emergencyData.value.map(item => Number(item.totalQty || 0))
      }
    ]
  }
})
useChart(emergencyLineRef, emergencyLineOpt)

const load = async () => {
  try {
    overview.value = await apiGet('/api/analytics/overview')
  } catch {
    overview.value = {
      userCount: 0,
      materialCount: 0,
      warehouseCount: 0,
      unhandledWarningCount: 0,
      pendingApplyCount: 0,
      pendingTransferCount: 0
    }
  }

  try { inventoryData.value = await apiGet('/api/analytics/inventory-ratio') } catch { inventoryData.value = [] }
  try { warehouseData.value = await apiGet('/api/analytics/warehouse-distribution') } catch { warehouseData.value = [] }
  try { trendData.value = await apiGet('/api/analytics/inbound-outbound-trend') } catch { trendData.value = [] }
  try { deptData.value = await apiGet('/api/analytics/department-ranking') } catch { deptData.value = [] }
  try { expiryData.value = await apiGet('/api/analytics/expiry-stats') } catch { expiryData.value = [] }
  try { emergencyData.value = await apiGet('/api/analytics/emergency-consumption') } catch { emergencyData.value = [] }
  await loadSmart()
}

const loadForecast = async () => {
  if (!canUseSmart.value || !forecastMaterialId.value) {
    forecastData.value = { history: [], movingAverage: 0, forecast: [] }
    return
  }
  try {
    forecastData.value = await apiGet('/api/smart/forecast', {
      materialId: forecastMaterialId.value,
      months: 3
    })
  } catch {
    forecastData.value = { history: [], movingAverage: 0, forecast: [] }
  }
}

const loadSmart = async () => {
  if (!canUseSmart.value) {
    replenishmentRows.value = []
    forecastOptions.value = []
    forecastMaterialId.value = null
    forecastData.value = { history: [], movingAverage: 0, forecast: [] }
    return
  }

  try {
    replenishmentRows.value = await apiGet('/api/smart/replenishment-suggestions', { guaranteeDays: 7 })
  } catch {
    replenishmentRows.value = []
  }

  try {
    forecastOptions.value = await apiGet('/api/material/info')
  } catch {
    forecastOptions.value = []
  }

  if (!forecastMaterialId.value) {
    forecastMaterialId.value = replenishmentRows.value[0]?.materialId || forecastOptions.value[0]?.id || null
  }
  await loadForecast()
}

onMounted(load)
</script>

<style scoped>
.chart-box {
  width: 100%;
  height: 300px;
}

.chart-empty {
  display: grid;
  place-items: center;
  min-height: 220px;
  color: var(--text-tertiary);
  text-align: center;
}

.smart-forecast {
  display: grid;
  gap: 14px;
}

.smart-forecast__summary {
  color: var(--text-secondary);
}

.smart-forecast__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 900px) {
  .smart-forecast__grid {
    grid-template-columns: 1fr;
  }
}
</style>
