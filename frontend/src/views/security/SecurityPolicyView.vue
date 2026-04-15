<template>
  <PageScaffold :metrics="metrics" page-type="workflow">
    <template #hero-actions>
      <el-button @click="loadTokenPolicy">刷新策略</el-button>
      <el-button type="warning" :loading="cleanupLoading" @click="triggerCleanup">一键手动清理</el-button>
    </template>

    <DetailSection title="管理员提示" description="仅管理员可查看与操作安全策略。">
      <el-alert title="仅管理员可查看与操作" type="info" :closable="false" />
    </DetailSection>

    <div class="surface-grid surface-grid--2">
      <DetailSection title="上次手动清理" description="本地缓存记录最近一次手动清理的结果。">
        <div v-if="lastCleanup" class="detail-grid">
          <div class="detail-item">
            <span class="detail-item__label">清理时间</span>
            <span class="detail-item__value">{{ lastCleanup.cleanedAt }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">移除数量</span>
            <span class="detail-item__value">{{ lastCleanup.removed }} 条</span>
          </div>
        </div>
        <EmptyState v-else compact glyph="SC" title="暂无缓存记录" description="尚未执行过手动清理。" />
      </DetailSection>

      <DetailSection title="Token 策略概览" description="查看多设备登录、过期时间与清理计划。">
        <div v-if="tokenPolicy" class="detail-grid">
          <div class="detail-item">
            <span class="detail-item__label">多设备登录</span>
            <div class="detail-item__value">
              <StatusBadge :label="tokenPolicy.multiDeviceLogin ? '开启' : '关闭（单设备）'" :tone="tokenPolicy.multiDeviceLogin ? 'success' : 'danger'" />
            </div>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">Access 过期</span>
            <span class="detail-item__value">{{ tokenPolicy.accessExpireMinutes }} 分钟</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">Refresh 过期</span>
            <span class="detail-item__value">{{ tokenPolicy.refreshExpireDays }} 天</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">清理 Cron</span>
            <span class="detail-item__value mono">{{ tokenPolicy.cleanupCron }}</span>
          </div>
        </div>
        <EmptyState v-else compact glyph="TK" title="暂无策略数据" description="请刷新策略以重新拉取 token 信息。" />
      </DetailSection>
    </div>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Clock, Key, Lock } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../api'
import DetailSection from '../../components/ui/DetailSection.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'

const tokenPolicy = ref(null)
const cleanupLoading = ref(false)
const lastCleanup = ref(null)
const LAST_CLEANUP_CACHE_KEY = 'security_policy_last_cleanup'

const metrics = computed(() => [
  { label: 'Token 总数', value: tokenPolicy.value?.tokenTotal ?? 0, helper: '当前 token 总量', icon: Key, tone: 'accent' },
  { label: '活跃 Token', value: tokenPolicy.value?.activeTokenCount ?? 0, helper: '当前仍有效的 token', icon: CircleCheck, tone: 'success' },
  { label: '已撤销 Token', value: tokenPolicy.value?.revokedTokenCount ?? 0, helper: '已被撤销的 token', icon: Lock, tone: 'danger' },
  { label: '已过期 Token', value: tokenPolicy.value?.expiredTokenCount ?? 0, helper: '已过期 token 数量', icon: Clock, tone: 'warning' }
])

const loadTokenPolicy = async () => {
  tokenPolicy.value = await apiGet('/api/auth/token-policy')
}

const triggerCleanup = async () => {
  cleanupLoading.value = true
  try {
    const data = await apiPost('/api/auth/token-cleanup', {})
    const cache = {
      cleanedAt: data?.cleanedAt || new Date().toLocaleString(),
      removed: data?.removed ?? 0
    }
    localStorage.setItem(LAST_CLEANUP_CACHE_KEY, JSON.stringify(cache))
    lastCleanup.value = cache
    ElMessage.success(`清理完成，移除 ${data?.removed ?? 0} 条 token 记录`)
    await loadTokenPolicy()
  } finally {
    cleanupLoading.value = false
  }
}

onMounted(() => {
  const cacheText = localStorage.getItem(LAST_CLEANUP_CACHE_KEY)
  if (cacheText) {
    try {
      lastCleanup.value = JSON.parse(cacheText)
    } catch (_) {
      localStorage.removeItem(LAST_CLEANUP_CACHE_KEY)
    }
  }
  loadTokenPolicy()
})
</script>
