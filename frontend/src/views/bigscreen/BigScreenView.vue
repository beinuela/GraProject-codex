<template>
  <div class="big-screen-container">
    <header class="header">
      <div class="header-left">
        <span class="time">{{ currentTime }}</span>
      </div>
      <div class="header-center">
        <h1>校园物资管理指挥中心</h1>
      </div>
      <div class="header-right">
        <el-button color="#1e40af" :dark="true" size="small" @click="goBack" class="back-btn">
          <el-icon><Back /></el-icon> 返回后台
        </el-button>
        <el-button color="#1e40af" :dark="true" size="small" @click="toggleFullScreen">
          <el-icon><FullScreen /></el-icon> 全屏切换
        </el-button>
      </div>
    </header>

    <main class="main-content">
      <!-- 左侧内容 -->
      <div class="side-panel left-panel">
        <div class="panel-box">
          <div class="panel-title">库存总览 (Stock)</div>
          <div class="kpi-row">
            <div class="kpi-item">
              <div class="kpi-label">物资总数</div>
              <div class="kpi-value text-cyan">{{ kpiData.totalItems || 0 }}</div>
            </div>
            <div class="kpi-item">
              <div class="kpi-label">预警总数</div>
              <div class="kpi-value text-red">{{ kpiData.activeWarnings || 0 }}</div>
            </div>
          </div>
          <div class="chart-container" ref="pieChartRef"></div>
        </div>

        <div class="panel-box">
          <div class="panel-title">出入库趋势 (Trend)</div>
          <div class="chart-container" ref="lineChartRef"></div>
        </div>
      </div>

      <!-- 中间内容 -->
      <div class="center-panel">
        <div class="map-container">
          <!-- 模拟大屏中心发光效果图/或Echarts地图 -->
          <div class="globe-mock">
            <div class="glow-circle"></div>
            <div class="glow-circle delay-1"></div>
            <div class="glow-circle delay-2"></div>
            <div class="center-text">
              <div class="val">{{ kpiData.monthlyEvents || 0 }}</div>
              <div class="lbl">本月事件列表</div>
            </div>
          </div>
        </div>
        
        <div class="panel-box bottom-box">
          <div class="panel-title">实时动态 (Live Events)</div>
          <div class="live-list">
            <div class="live-item" v-for="i in 3" :key="i">
              <span class="live-time">{{ currentTime }}</span>
              <span class="live-text text-cyan">[系统广播]</span>
              <span class="live-detail">中心仓库完成自动环境温湿度检测，状态正常。</span>
            </div>
             <div class="live-item" v-if="kpiData.pendingApprovals > 0">
              <span class="live-time">{{ currentTime }}</span>
              <span class="live-text text-yellow">[审批待办]</span>
              <span class="live-detail">发现 {{ kpiData.pendingApprovals }} 个待审核申领单，请及时处理。</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="side-panel right-panel">
        <div class="panel-box">
          <div class="panel-title">部门领用排行 (Top Depts)</div>
          <div class="chart-container" ref="barChartRef"></div>
        </div>

        <div class="panel-box">
          <div class="panel-title">保质期监控 (Expiry)</div>
          <div class="chart-container" ref="expiryChartRef"></div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { FullScreen, Back } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { apiGet } from '../../api'
import * as echarts from 'echarts'

const router = useRouter()
const currentTime = ref('')
let timer = null

const kpiData = ref({})
const pieChartRef = ref(null)
const lineChartRef = ref(null)
const barChartRef = ref(null)
const expiryChartRef = ref(null)
const charts = []

const goBack = () => {
  router.push('/dashboard')
}

const toggleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen()
    }
  }
}

const initCharts = async () => {
  try {
    const overview = await apiGet('/api/analytics/overview')
    kpiData.value = overview

    const ratioData = await apiGet('/api/analytics/inventory-ratio')
    const mcType = pieChartRef.value
    if (mcType) {
      const chart = echarts.init(mcType, 'dark')
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item' },
        series: [{
          name: '占比', type: 'pie', radius: ['40%', '70%'],
          itemStyle: { borderRadius: 4, borderColor: '#0b0f19', borderWidth: 2 },
          data: ratioData.map(d => ({ name: d.name, value: Number(d.value) }))
        }]
      })
      charts.push(chart)
    }

    const trendData = await apiGet('/api/analytics/inbound-outbound-trend')
    const mcTrend = lineChartRef.value
    if (mcTrend) {
      const chart = echarts.init(mcTrend, 'dark')
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis' },
        legend: { textStyle: { color: '#ccc' } },
        grid: { left: 40, right: 10, top: 30, bottom: 20 },
        xAxis: { type: 'category', data: trendData.map(d => d.monthKey || d.month_key), axisLabel: { color: '#8b9baf' } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1a2639' } }, axisLabel: { color: '#8b9baf' } },
        color: ['#06b6d4', '#f43f5e'],
        series: [
          { name: '入库', type: 'line', smooth: true, areaStyle: { opacity: 0.2 }, data: trendData.map(d => Number(d.inQty || d.in_qty || 0)) },
          { name: '出库', type: 'line', smooth: true, areaStyle: { opacity: 0.2 }, data: trendData.map(d => Number(d.outQty || d.out_qty || 0)) }
        ]
      })
      charts.push(chart)
    }

    const deptData = await apiGet('/api/analytics/department-ranking')
    const mcBar = barChartRef.value
    if (mcBar) {
      const chart = echarts.init(mcBar, 'dark')
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis' },
        grid: { left: 80, right: 10, top: 10, bottom: 20 },
        xAxis: { type: 'value', splitLine: { show: false }, axisLabel: { color: '#8b9baf' } },
        yAxis: { type: 'category', data: deptData.map(d => d.deptName), axisLabel: { color: '#cbd5e1' } },
        series: [{
          type: 'bar', data: deptData.map(d => Number(d.totalQty)),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#06b6d4' }]),
            borderRadius: [0, 4, 4, 0]
          }
        }]
      })
      charts.push(chart)
    }

    const expiryData = await apiGet('/api/analytics/expiry-stats')
    const mcExp = expiryChartRef.value
    if (mcExp) {
      const chart = echarts.init(mcExp, 'dark')
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis' },
        legend: { textStyle: { color: '#ccc' } },
        grid: { left: 40, right: 10, top: 30, bottom: 40 },
        xAxis: { type: 'category', data: expiryData.map(d => d.materialName), axisLabel: { color: '#8b9baf', rotate: 30, fontSize: 10 } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1a2639' } }, axisLabel: { color: '#8b9baf' } },
        color: ['#ef4444', '#f59e0b'],
        series: [
          { name: '已过期', type: 'bar', stack: 'total', data: expiryData.map(d => Number(d.expiredQty)) },
          { name: '将要过期', type: 'bar', stack: 'total', data: expiryData.map(d => Number(d.expiringSoonQty)) }
        ]
      })
      charts.push(chart)
    }

  } catch (err) {
    console.error('BigScreen load error', err)
  }
}

const handleResize = () => {
  charts.forEach(c => c.resize())
}

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  }, 1000)
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped>
.big-screen-container {
  width: 100vw;
  height: 100vh;
  background: #0b0f19 radial-gradient(circle at center, #111827 0%, #0b0f19 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.header {
  height: 80px;
  background: url('data:image/svg+xml;utf8,<svg width="100%" height="100%" xmlns="http://www.w3.org/2000/svg"><path d="M0,80 L0,0 L100%,0 L100%,80 L80%,80 L75%,40 L25%,40 L20%,80 Z" fill="rgba(14, 165, 233, 0.05)" stroke="rgba(14, 165, 233, 0.3)" stroke-width="2"/></svg>') no-repeat center bottom;
  background-size: 100% 100%;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 0 40px;
  position: relative;
}

.header-left, .header-right {
  width: 300px;
  margin-top: 20px;
  display: flex;
  align-items: center;
}
.header-right {
  justify-content: flex-end;
  gap: 10px;
}
.header-center h1 {
  margin: 0;
  padding-top: 15px;
  font-size: 28px;
  font-weight: bold;
  letter-spacing: 4px;
  color: #e2e8f0;
  text-shadow: 0 0 10px rgba(14, 165, 233, 0.5);
  text-align: center;
}
.time {
  font-size: 20px;
  color: #0ea5e9;
  font-family: monospace;
}

.main-content {
  flex: 1;
  display: flex;
  padding: 20px;
  gap: 20px;
}

.side-panel {
  width: 400px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.center-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-box {
  flex: 1;
  background: rgba(14, 25, 43, 0.6);
  border: 1px solid rgba(14, 165, 233, 0.2);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  padding: 15px;
  position: relative;
  box-shadow: 0 0 15px rgba(0,0,0,0.5) inset;
}
.panel-box::before, .panel-box::after {
  content: '';
  position: absolute;
  width: 15px; height: 15px;
  border-color: #0ea5e9; border-style: solid;
}
.panel-box::before { top: -1px; left: -1px; border-width: 2px 0 0 2px; }
.panel-box::after { bottom: -1px; right: -1px; border-width: 0 2px 2px 0; }

.panel-title {
  font-size: 16px;
  font-weight: bold;
  color: #38bdf8;
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid #0ea5e9;
  letter-spacing: 1px;
}

.chart-container {
  flex: 1;
  width: 100%;
}

.kpi-row {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
}
.kpi-item {
  flex: 1;
  background: rgba(255,255,255,0.02);
  padding: 10px;
  border-radius: 4px;
  text-align: center;
}
.kpi-label { font-size: 12px; color: #94a3b8; }
.kpi-value { font-size: 28px; font-weight: bold; font-family: monospace; line-height: 1.2; }

.text-cyan { color: #22d3ee; }
.text-red { color: #f87171; }
.text-yellow { color: #facc15; }

/* 中心地球模拟效果 */
.map-container {
  flex: 2;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
}
.globe-mock {
  width: 300px;
  height: 300px;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
}
.glow-circle {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid rgba(14, 165, 233, 0.4);
  box-shadow: 0 0 20px rgba(14, 165, 233, 0.2) inset, 0 0 20px rgba(14, 165, 233, 0.2);
  animation: pulse 3s infinite linear;
}
.delay-1 { animation-delay: -1s; width: 70%; height: 70%; border-color: rgba(56, 189, 248, 0.6); }
.delay-2 { animation-delay: -2s; width: 40%; height: 40%; border-color: rgba(34, 211, 238, 0.8); }

@keyframes pulse {
  0% { transform: scale(0.8) rotateX(60deg) rotateZ(0deg); opacity: 1; }
  100% { transform: scale(1.5) rotateX(60deg) rotateZ(360deg); opacity: 0; }
}

.center-text {
  text-align: center;
  z-index: 10;
  background: rgba(11, 15, 25, 0.8);
  padding: 20px;
  border-radius: 50%;
  border: 1px solid rgba(14, 165, 233, 0.5);
  box-shadow: 0 0 15px rgba(14, 165, 233, 0.5);
}
.center-text .val { font-size: 36px; font-weight: bold; color: #38bdf8; }
.center-text .lbl { font-size: 14px; color: #e2e8f0; }

.bottom-box { flex: 1; }
.live-list {
  flex: 1;
  overflow-y: auto;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.live-item {
  background: rgba(255,255,255,0.03);
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  gap: 10px;
}
.live-time { color: #64748b; font-family: monospace; }
.live-detail { color: #cbd5e1; }
</style>
