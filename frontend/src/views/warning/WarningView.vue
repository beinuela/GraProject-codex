<template>
  <PageScaffold :metrics="metrics" page-type="monitor">
    <FilterActionBar>
      <template #filters>
        <el-select v-model="filterType" clearable placeholder="预警类型" style="width: 180px" @change="applyFilters">
          <el-option value="STOCK_LOW" label="库存不足" />
          <el-option value="STOCK_BACKLOG" label="库存积压" />
          <el-option value="EXPIRING_SOON" label="即将过期" />
          <el-option value="EXPIRED" label="已过期" />
          <el-option value="ABNORMAL_USAGE" label="异常消耗" />
        </el-select>
        <el-select v-model="filterStatus" clearable placeholder="处理状态" style="width: 160px" @change="applyFilters">
          <el-option value="UNHANDLED" label="未处理" />
          <el-option value="HANDLED" label="已处理" />
        </el-select>
      </template>
      <template #actions>
        <el-button @click="load">刷新</el-button>
        <el-button type="warning" @click="triggerScan">手动扫描</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="预警中心" description="查看预警类型、状态和处置动作。" :badge="`${pagination.total} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="预警类型" width="140">
          <template #default="{ row }">
            <StatusBadge :label="typeLabel(row.warningType)" :tone="typeTone(row.warningType)" />
          </template>
        </el-table-column>
        <el-table-column prop="materialId" label="物资ID" width="100" />
        <el-table-column prop="warehouseId" label="仓库ID" width="100" />
        <el-table-column prop="content" label="预警内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <StatusBadge :label="row.handleStatus === 'HANDLED' ? '已处理' : '未处理'" :tone="row.handleStatus === 'HANDLED' ? 'success' : 'danger'" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" :loading="analyzingId === row.id" @click="analyzeWarning(row)">AI 分析</el-button>
              <el-button v-if="row.handleStatus === 'UNHANDLED'" size="small" type="primary" @click="handleWarning(row.id)">处理</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="WR" title="暂无预警记录" description="当前筛选范围内没有预警项。" />
        </template>
      </el-table>
    </TableShell>
    <PaginationBar
      :page="pagination.page"
      :size="pagination.size"
      :total="pagination.total"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <DialogShell
      v-model="analysisVisible"
      title="AI 预警分析"
      eyebrow="DeepSeek Assist"
      :subtitle="analysisSubtitle"
      width="980"
    >
      <div v-if="analysisTask" class="surface-grid surface-grid--2">
        <DetailSection title="分析概览" description="记录任务来源、风险等级与摘要判断。">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">任务 ID</span>
              <span class="detail-item__value mono">{{ analysisTask.taskId }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">分析来源</span>
              <div class="detail-item__value">
                <StatusBadge :label="sourceLabel(analysisTask.source)" :tone="sourceTone(analysisTask.source)" />
              </div>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">风险等级</span>
              <div class="detail-item__value">
                <StatusBadge :label="riskLabel(analysisTask.result?.riskLevel)" :tone="riskTone(analysisTask.result?.riskLevel)" />
              </div>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">建议时限</span>
              <span class="detail-item__value">{{ deadlineText(analysisTask.result?.deadlineHours) }}</span>
            </div>
          </div>
          <div class="analysis-summary">
            {{ analysisTask.result?.summary || '暂无分析摘要。' }}
          </div>
        </DetailSection>

        <DetailSection title="处置建议" description="列出可能原因、建议动作与责任角色。">
          <div class="analysis-list-block">
            <span class="analysis-list-block__title">可能原因</span>
            <ul class="analysis-list">
              <li v-for="item in analysisTask.result?.possibleCauses || []" :key="`cause-${item}`">{{ item }}</li>
            </ul>
          </div>
          <div class="analysis-list-block">
            <span class="analysis-list-block__title">建议动作</span>
            <ul class="analysis-list">
              <li v-for="item in analysisTask.result?.actions || []" :key="`action-${item}`">{{ item }}</li>
            </ul>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">建议责任角色</span>
            <span class="detail-item__value">{{ ownerRoleLabel(analysisTask.result?.ownerRole) }}</span>
          </div>
        </DetailSection>
      </div>
      <template #footer>
        <el-button @click="analysisVisible = false">关闭</el-button>
      </template>
    </DialogShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, DataAnalysis, Warning } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../api'
import DetailSection from '../../components/ui/DetailSection.vue'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PaginationBar from '../../components/ui/PaginationBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const filterType = ref('')
const filterStatus = ref('')
const analysisVisible = ref(false)
const analyzingId = ref(null)
const analysisTask = ref(null)
const selectedWarning = ref(null)
const pagination = reactive({ page: 1, size: 10, total: 0 })

const typeLabel = type => ({ STOCK_LOW: '库存不足', STOCK_BACKLOG: '库存积压', EXPIRING_SOON: '即将过期', EXPIRED: '已过期', ABNORMAL_USAGE: '异常消耗' }[type] || type)
const typeTone = type => ({ STOCK_LOW: 'danger', STOCK_BACKLOG: 'warning', EXPIRING_SOON: 'warning', EXPIRED: 'danger', ABNORMAL_USAGE: 'accent' }[type] || 'neutral')
const riskLabel = level => ({ LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险', CRITICAL: '极高风险' }[level] || level || '待确认')
const riskTone = level => ({ LOW: 'success', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }[level] || 'neutral')
const sourceLabel = source => ({ LLM: 'DeepSeek', RULE_FALLBACK: '规则回退' }[source] || source || '未知')
const sourceTone = source => ({ LLM: 'accent', RULE_FALLBACK: 'warning' }[source] || 'neutral')
const ownerRoleLabel = role => ({ ADMIN: '系统管理员', WAREHOUSE_ADMIN: '仓库管理员', APPROVER: '审批人' }[role] || role || '仓库管理员')
const deadlineText = deadlineHours => deadlineHours ? `${deadlineHours} 小时内` : '按业务紧急度尽快处理'

const metrics = computed(() => [
  { label: '预警总数', value: pagination.total, helper: '当前筛选范围内预警总量', icon: Bell, tone: 'accent' },
  { label: '未处理', value: list.value.filter(item => item.handleStatus === 'UNHANDLED').length, helper: '待人工处置', icon: Warning, tone: 'danger' },
  { label: '已处理', value: list.value.filter(item => item.handleStatus === 'HANDLED').length, helper: '当前页已完成闭环', icon: DataAnalysis, tone: 'success' }
])
const analysisSubtitle = computed(() => {
  if (!selectedWarning.value) {
    return '基于库存、批次、出库波动与积压压力生成处置建议。'
  }
  return `${typeLabel(selectedWarning.value.warningType)} · 物资ID ${selectedWarning.value.materialId || '-'} · 仓库ID ${selectedWarning.value.warehouseId || '-'}`
})

const load = async () => {
  const result = await apiGet('/api/warning/list', {
    page: pagination.page,
    size: pagination.size,
    type: filterType.value || undefined,
    status: filterStatus.value || undefined
  })
  list.value = result.records || []
  pagination.total = Number(result.total || 0)
}

const triggerScan = async () => {
  await apiPost('/api/warning/scan')
  ElMessage.success('预警扫描已完成')
  await load()
}

const handleWarning = async (id) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入处理备注', '处理预警', { confirmButtonText: '确定', cancelButtonText: '取消' })
    await apiPost(`/api/warning/${id}/handle`, { remark: value })
    ElMessage.success('预警已处理')
    await load()
  } catch (_) {
    // 用户取消时不提示错误。
  }
}

const analyzeWarning = async (row) => {
  analyzingId.value = row.id
  selectedWarning.value = row
  try {
    analysisTask.value = await apiPost(`/api/ai/warnings/${row.id}/analyze`, {})
    analysisVisible.value = true
    ElMessage.success(analysisTask.value?.source === 'LLM' ? 'DeepSeek 分析完成' : 'AI 分析已完成，当前展示规则回退结果')
  } finally {
    analyzingId.value = null
  }
}

const handlePageChange = async (page) => {
  pagination.page = page
  await load()
}

const handleSizeChange = async (size) => {
  pagination.size = size
  pagination.page = 1
  await load()
}

const applyFilters = async () => {
  pagination.page = 1
  await load()
}

onMounted(load)
</script>

<style scoped>
.analysis-summary {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(38, 112, 233, 0.06);
  color: var(--text-primary);
  line-height: 1.8;
}

.analysis-list-block {
  display: grid;
  gap: 10px;
}

.analysis-list-block__title {
  font-weight: 700;
  color: var(--text-primary);
}

.analysis-list {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
  color: var(--text-secondary);
  line-height: 1.7;
}
</style>
