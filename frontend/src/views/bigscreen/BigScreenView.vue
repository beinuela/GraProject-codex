<template>
  <div class="screen-shell">
    <header class="screen-topbar">
      <div class="screen-topbar__brand">
        <span class="screen-topbar__eyebrow">Campus Material Command</span>
        <h1>校园物资管理指挥中心</h1>
        <p>统一观察库存、调拨、申领、预警与事件态势。</p>
      </div>

      <div class="screen-topbar__meta">
        <div class="screen-clock">
          <span>系统时间</span>
          <strong>{{ currentTime }}</strong>
        </div>
        <div class="inline-actions">
          <el-button @click="goBack">
            <el-icon><Back /></el-icon>
            返回后台
          </el-button>
          <el-button type="primary" @click="toggleFullScreen">
            <el-icon><FullScreen /></el-icon>
            全屏切换
          </el-button>
        </div>
      </div>
    </header>

    <section class="screen-summary">
      <article v-for="card in overviewCards" :key="card.label" class="screen-metric" :class="`screen-metric--${card.tone}`">
        <span class="screen-metric__label">{{ card.label }}</span>
        <strong class="screen-metric__value">{{ card.value }}</strong>
        <span class="screen-metric__helper">{{ card.helper }}</span>
      </article>
    </section>

    <main class="screen-grid">
      <div class="screen-column">
        <section class="screen-panel">
          <header class="screen-panel__header">
            <div>
              <span class="screen-panel__eyebrow">Inventory</span>
              <h2>库存结构</h2>
            </div>
          </header>
          <div ref="pieChartRef" class="screen-chart"></div>
        </section>

        <section class="screen-panel">
          <header class="screen-panel__header">
            <div>
              <span class="screen-panel__eyebrow">Trend</span>
              <h2>出入库趋势</h2>
            </div>
          </header>
          <div ref="lineChartRef" class="screen-chart"></div>
        </section>
      </div>

      <div class="screen-center">
        <section class="screen-hero">
          <div class="screen-hero__orb">
            <div class="screen-hero__ring"></div>
            <div class="screen-hero__ring screen-hero__ring--inner"></div>
            <div class="screen-hero__core">
              <strong>{{ monthlyEvents }}</strong>
              <span>本月事件</span>
            </div>
          </div>
          <div class="screen-hero__stats">
            <article>
              <span>待审批流程</span>
              <strong>{{ pendingApprovals }}</strong>
            </article>
            <article>
              <span>仓储节点</span>
              <strong>{{ warehouseCount }}</strong>
            </article>
          </div>
        </section>

        <section class="screen-panel screen-panel--live">
          <header class="screen-panel__header">
            <div>
              <span class="screen-panel__eyebrow">Live Feed</span>
              <h2>实时动态</h2>
            </div>
          </header>
          <div class="screen-feed">
            <article v-for="item in liveFeed" :key="item.title" class="screen-feed__item">
              <span class="screen-feed__time">{{ currentTime }}</span>
              <div class="screen-feed__content">
                <strong>{{ item.title }}</strong>
                <p>{{ item.detail }}</p>
              </div>
            </article>
          </div>
        </section>
      </div>

      <div class="screen-column">
        <section class="screen-panel">
          <header class="screen-panel__header">
            <div>
              <span class="screen-panel__eyebrow">Departments</span>
              <h2>部门领用排行</h2>
            </div>
          </header>
          <div ref="barChartRef" class="screen-chart"></div>
        </section>

        <section class="screen-panel">
          <header class="screen-panel__header">
            <div>
              <span class="screen-panel__eyebrow">Expiry</span>
              <h2>效期监控</h2>
            </div>
          </header>
          <div ref="expiryChartRef" class="screen-chart"></div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Back, FullScreen } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import { apiGet } from '../../api'

const router = useRouter()
const currentTime = ref('')
const overview = ref({})
const charts = []

const pieChartRef = ref(null)
const lineChartRef = ref(null)
const barChartRef = ref(null)
const expiryChartRef = ref(null)

let timer = null

const totalItems = computed(() => overview.value.totalItems ?? overview.value.materialCount ?? 0)
const activeWarnings = computed(() => overview.value.activeWarnings ?? overview.value.unhandledWarningCount ?? 0)
const pendingApprovals = computed(() => overview.value.pendingApprovals ?? (overview.value.pendingApplyCount ?? 0) + (overview.value.pendingTransferCount ?? 0))
const monthlyEvents = computed(() => overview.value.monthlyEvents ?? overview.value.eventCount ?? 0)
const warehouseCount = computed(() => overview.value.warehouseCount ?? 0)

const overviewCards = computed(() => [
  { label: '物资总数', value: totalItems.value, helper: '已纳入总控台账', tone: 'accent' },
  { label: '活跃预警', value: activeWarnings.value, helper: '待持续跟踪', tone: activeWarnings.value ? 'danger' : 'teal' },
  { label: '待审批流程', value: pendingApprovals.value, helper: '申领与调拨待办', tone: 'warning' },
  { label: '仓储节点', value: warehouseCount.value, helper: '当前在线仓库', tone: 'neutral' }
])

const liveFeed = computed(() => [
  { title: '库存联动', detail: `当前纳管物资 ${totalItems.value} 项，仓储节点 ${warehouseCount.value} 个。` },
  { title: '风险监测', detail: `系统检测到 ${activeWarnings.value} 条活跃预警，建议优先处理高风险事项。` },
  { title: '流程待办', detail: `当前共有 ${pendingApprovals.value} 个流程待审批，建议在后台尽快闭环。` }
])

const chartText = '#9bb0d0'
const chartGrid = '#18304d'

const updateClock = () => {
  currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
}

const goBack = () => router.push('/dashboard')

const toggleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    return
  }
  if (document.exitFullscreen) {
    document.exitFullscreen()
  }
}

const buildCharts = async () => {
  charts.forEach(chart => chart.dispose())
  charts.length = 0

  try {
    overview.value = await apiGet('/api/analytics/overview')

    const [ratioData, trendData, deptData, expiryData] = await Promise.all([
      apiGet('/api/analytics/inventory-ratio'),
      apiGet('/api/analytics/inbound-outbound-trend'),
      apiGet('/api/analytics/department-ranking'),
      apiGet('/api/analytics/expiry-stats')
    ])

    if (pieChartRef.value) {
      const chart = echarts.init(pieChartRef.value)
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item', borderRadius: 14 },
        color: ['#35d4c6', '#2670e9', '#7856ff', '#ff9f43', '#fa5252'],
        legend: { bottom: 0, textStyle: { color: chartText } },
        series: [
          {
            type: 'pie',
            radius: ['38%', '68%'],
            center: ['50%', '42%'],
            itemStyle: { borderRadius: 10, borderColor: '#07111f', borderWidth: 3 },
            label: { show: false },
            data: ratioData.map(item => ({ name: item.name, value: Number(item.value) }))
          }
        ]
      })
      charts.push(chart)
    }

    if (lineChartRef.value) {
      const chart = echarts.init(lineChartRef.value)
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', borderRadius: 14 },
        legend: { textStyle: { color: chartText } },
        grid: { left: 42, right: 14, top: 34, bottom: 26 },
        xAxis: {
          type: 'category',
          data: trendData.map(item => item.monthKey || item.month_key),
          axisLabel: { color: chartText },
          axisLine: { lineStyle: { color: chartGrid } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: chartText },
          splitLine: { lineStyle: { color: chartGrid } }
        },
        color: ['#35d4c6', '#fa5252'],
        series: [
          { name: '入库', type: 'line', smooth: true, symbolSize: 8, areaStyle: { opacity: 0.14 }, data: trendData.map(item => Number(item.inQty || item.in_qty || 0)) },
          { name: '出库', type: 'line', smooth: true, symbolSize: 8, areaStyle: { opacity: 0.14 }, data: trendData.map(item => Number(item.outQty || item.out_qty || 0)) }
        ]
      })
      charts.push(chart)
    }

    if (barChartRef.value) {
      const chart = echarts.init(barChartRef.value)
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, borderRadius: 14 },
        grid: { left: 92, right: 12, top: 14, bottom: 20 },
        xAxis: { type: 'value', axisLabel: { color: chartText }, splitLine: { lineStyle: { color: chartGrid } } },
        yAxis: { type: 'category', data: deptData.map(item => item.deptName), axisLabel: { color: chartText } },
        series: [
          {
            type: 'bar',
            barWidth: '56%',
            data: deptData.map(item => Number(item.totalQty)),
            itemStyle: {
              borderRadius: [0, 10, 10, 0],
              color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
                { offset: 0, color: '#35d4c6' },
                { offset: 1, color: '#2670e9' }
              ])
            }
          }
        ]
      })
      charts.push(chart)
    }

    if (expiryChartRef.value) {
      const chart = echarts.init(expiryChartRef.value)
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', borderRadius: 14 },
        legend: { textStyle: { color: chartText } },
        grid: { left: 44, right: 12, top: 34, bottom: 44 },
        xAxis: {
          type: 'category',
          data: expiryData.map(item => item.materialName),
          axisLabel: { color: chartText, rotate: 20, fontSize: 11 },
          axisLine: { lineStyle: { color: chartGrid } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: chartText },
          splitLine: { lineStyle: { color: chartGrid } }
        },
        color: ['#fa5252', '#ff9f43'],
        series: [
          { name: '已过期', type: 'bar', stack: 'total', data: expiryData.map(item => Number(item.expiredQty || 0)) },
          { name: '即将过期', type: 'bar', stack: 'total', data: expiryData.map(item => Number(item.expiringSoonQty || item.expiringQty || 0)) }
        ]
      })
      charts.push(chart)
    }
  } catch (error) {
    console.error('BigScreen load error', error)
  }
}

const handleResize = () => {
  charts.forEach(chart => chart.resize())
}

onMounted(() => {
  updateClock()
  timer = window.setInterval(updateClock, 1000)
  buildCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
  window.removeEventListener('resize', handleResize)
  charts.forEach(chart => chart.dispose())
})
</script>

<style scoped>
.screen-shell {
  min-height: 100vh;
  padding: 18px;
  background:
    radial-gradient(circle at top center, rgba(38, 112, 233, 0.16), transparent 28%),
    linear-gradient(180deg, #050b15 0%, #081121 100%);
  color: rgba(240, 246, 255, 0.92);
}

.screen-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px;
  border-radius: 28px;
  border: 1px solid rgba(112, 160, 255, 0.1);
  background: rgba(9, 18, 33, 0.82);
  box-shadow: 0 28px 70px rgba(1, 4, 10, 0.38);
}

.screen-topbar__eyebrow,
.screen-panel__eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(53, 212, 198, 0.12);
  color: #7ee8de;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.screen-topbar__brand h1 {
  margin: 12px 0 8px;
  font-family: var(--font-display);
  font-size: clamp(34px, 5vw, 54px);
  line-height: 0.96;
  letter-spacing: -0.05em;
}

.screen-topbar__brand p {
  margin: 0;
  color: rgba(173, 194, 224, 0.76);
}

.screen-topbar__meta {
  display: grid;
  gap: 14px;
  justify-items: end;
}

.screen-clock {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.screen-clock span {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(173, 194, 224, 0.68);
}

.screen-clock strong {
  font-family: var(--font-mono);
  color: #fff;
}

.screen-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.screen-metric {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 24px;
  border: 1px solid rgba(112, 160, 255, 0.1);
  background: linear-gradient(180deg, rgba(9, 18, 33, 0.92), rgba(6, 12, 24, 0.94));
}

.screen-metric__label {
  color: rgba(173, 194, 224, 0.68);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.screen-metric__value {
  font-family: var(--font-display);
  font-size: clamp(30px, 3vw, 42px);
  line-height: 1;
  letter-spacing: -0.04em;
}

.screen-metric__helper {
  color: rgba(173, 194, 224, 0.76);
  font-size: 13px;
}

.screen-metric--accent .screen-metric__value {
  color: #7cb4ff;
}

.screen-metric--teal .screen-metric__value {
  color: #7ee8de;
}

.screen-metric--warning .screen-metric__value {
  color: #ffcb7f;
}

.screen-metric--danger .screen-metric__value {
  color: #ff9d9d;
}

.screen-metric--neutral .screen-metric__value {
  color: #eef4ff;
}

.screen-grid {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(360px, 1.15fr) minmax(280px, 1fr);
  gap: 16px;
  margin-top: 16px;
  min-height: calc(100vh - 240px);
}

.screen-column,
.screen-center {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.screen-panel,
.screen-hero {
  display: grid;
  gap: 14px;
  border-radius: 28px;
  border: 1px solid rgba(112, 160, 255, 0.1);
  background: linear-gradient(180deg, rgba(9, 18, 33, 0.9), rgba(6, 12, 24, 0.94));
  padding: 20px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.02);
}

.screen-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.screen-panel__header h2 {
  margin: 10px 0 0;
  font-size: 22px;
  color: #eef4ff;
}

.screen-chart {
  width: 100%;
  height: 320px;
}

.screen-center {
  grid-template-rows: minmax(320px, 1fr) minmax(240px, auto);
}

.screen-hero {
  align-items: center;
  justify-items: center;
  background:
    radial-gradient(circle at center, rgba(38, 112, 233, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(9, 18, 33, 0.92), rgba(5, 11, 22, 0.96));
}

.screen-hero__orb {
  position: relative;
  display: grid;
  place-items: center;
  width: min(64vw, 320px);
  aspect-ratio: 1;
}

.screen-hero__ring {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(124, 180, 255, 0.34);
  border-radius: 999px;
  box-shadow: 0 0 40px rgba(38, 112, 233, 0.18);
  animation: pulse 4s linear infinite;
}

.screen-hero__ring--inner {
  inset: 16%;
  border-color: rgba(126, 232, 222, 0.5);
  animation-duration: 3s;
  animation-direction: reverse;
}

.screen-hero__core {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 6px;
  place-items: center;
  width: 52%;
  aspect-ratio: 1;
  border-radius: 999px;
  background: radial-gradient(circle at center, rgba(38, 112, 233, 0.4), rgba(6, 12, 24, 0.94));
  box-shadow: 0 0 60px rgba(38, 112, 233, 0.28);
}

.screen-hero__core strong {
  font-family: var(--font-display);
  font-size: clamp(42px, 5vw, 68px);
  line-height: 1;
  color: #fff;
}

.screen-hero__core span {
  color: rgba(184, 204, 232, 0.78);
}

.screen-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.screen-hero__stats article {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.04);
}

.screen-hero__stats span {
  color: rgba(173, 194, 224, 0.68);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.screen-hero__stats strong {
  font-family: var(--font-display);
  font-size: 28px;
  color: #eef4ff;
}

.screen-feed {
  display: grid;
  gap: 12px;
}

.screen-feed__item {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.screen-feed__time {
  font-family: var(--font-mono);
  font-size: 12px;
  color: rgba(173, 194, 224, 0.64);
}

.screen-feed__content {
  display: grid;
  gap: 4px;
}

.screen-feed__content strong {
  color: #eef4ff;
}

.screen-feed__content p {
  margin: 0;
  color: rgba(173, 194, 224, 0.78);
  line-height: 1.6;
}

@keyframes pulse {
  from {
    transform: scale(0.92);
    opacity: 0.6;
  }
  to {
    transform: scale(1.08);
    opacity: 0.18;
  }
}

@media (max-width: 1280px) {
  .screen-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .screen-grid {
    grid-template-columns: 1fr;
  }

  .screen-center {
    grid-template-rows: auto;
  }
}

@media (max-width: 768px) {
  .screen-shell {
    padding: 12px;
  }

  .screen-topbar {
    flex-direction: column;
    padding: 18px;
  }

  .screen-topbar__meta {
    width: 100%;
    justify-items: start;
  }

  .screen-summary,
  .screen-hero__stats {
    grid-template-columns: 1fr;
  }

  .screen-feed__item {
    grid-template-columns: 1fr;
  }
}
</style>
