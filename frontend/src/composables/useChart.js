import { onMounted, onBeforeUnmount, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'

/**
 * ECharts 实例管理 Composable
 * @param {import('vue').Ref<HTMLElement>} elRef - 图表容器的 template ref
 * @param {import('vue').Ref<Object>|import('vue').ComputedRef<Object>} optionsRef - ECharts 配置项 (响应式)
 */
export function useChart(elRef, optionsRef) {
  const chartInstance = shallowRef(null)

  const initChart = () => {
    if (!elRef.value) return
    chartInstance.value = echarts.init(elRef.value)
    if (optionsRef.value) {
      chartInstance.value.setOption(optionsRef.value)
    }
  }

  const handleResize = () => {
    chartInstance.value?.resize()
  }

  onMounted(() => {
    initChart()
    window.addEventListener('resize', handleResize)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
    chartInstance.value?.dispose()
  })

  // 当配置项变化时自动重绘
  watch(optionsRef, (newOpt) => {
    if (!chartInstance.value) {
      initChart()
    }
    if (chartInstance.value && newOpt) {
      chartInstance.value.setOption(newOpt, true)
    }
  }, { deep: true })

  return { chartInstance }
}
