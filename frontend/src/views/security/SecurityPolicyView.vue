<template>
  <div class="page-card">
    <h2 class="page-title">安全策略</h2>

    <el-card shadow="never" style="margin-bottom: 12px; border-color: #e6f4ff; background: #f8fcff;">
      <template #header>
        <div style="font-weight: 600; color: #1f2937;">本地提示：上次手动清理</div>
      </template>
      <div v-if="lastCleanup" style="font-size: 13px; color: #475569; line-height: 1.8;">
        <div>时间：{{ lastCleanup.cleanedAt }}</div>
        <div>清理数量：{{ lastCleanup.removed }} 条</div>
      </div>
      <div v-else style="font-size: 13px; color: #94a3b8;">暂无本地缓存记录</div>
    </el-card>

    <el-alert
      title="仅管理员可查看与操作"
      type="info"
      :closable="false"
      style="margin-bottom: 12px"
    />

    <el-space style="margin-bottom:12px">
      <el-button @click="loadTokenPolicy">刷新策略</el-button>
      <el-button type="warning" :loading="cleanupLoading" @click="triggerCleanup">一键手动清理</el-button>
    </el-space>

    <el-descriptions v-if="tokenPolicy" :column="2" border>
      <el-descriptions-item label="多设备登录">
        <el-tag :type="tokenPolicy.multiDeviceLogin ? 'success' : 'danger'">
          {{ tokenPolicy.multiDeviceLogin ? '开启' : '关闭（单设备）' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="Access 过期（分钟）">{{ tokenPolicy.accessExpireMinutes }}</el-descriptions-item>
      <el-descriptions-item label="Refresh 过期（天）">{{ tokenPolicy.refreshExpireDays }}</el-descriptions-item>
      <el-descriptions-item label="清理 Cron">{{ tokenPolicy.cleanupCron }}</el-descriptions-item>
      <el-descriptions-item label="Token 总数">{{ tokenPolicy.tokenTotal }}</el-descriptions-item>
      <el-descriptions-item label="活跃 Token">{{ tokenPolicy.activeTokenCount }}</el-descriptions-item>
      <el-descriptions-item label="已撤销 Token">{{ tokenPolicy.revokedTokenCount }}</el-descriptions-item>
      <el-descriptions-item label="已过期 Token">{{ tokenPolicy.expiredTokenCount }}</el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet, apiPost } from '../../api'

const tokenPolicy = ref(null)
const cleanupLoading = ref(false)
const lastCleanup = ref(null)
const LAST_CLEANUP_CACHE_KEY = 'security_policy_last_cleanup'

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
