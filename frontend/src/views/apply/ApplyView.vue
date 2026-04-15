<template>
  <PageScaffold :metrics="metrics" page-type="workflow">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">申领流程保留原有提交、审批、驳回与签收动作，只重构呈现层级。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新建申领</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="申领单列表" description="查看申领单状态、紧急程度与业务动作。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="deptId" label="部门ID" width="100" />
        <el-table-column prop="applicantId" label="申请人ID" width="110" />
        <el-table-column label="紧急程度" width="120">
          <template #default="{ row }">
            <StatusBadge :label="urgencyLabel(row.urgencyLevel)" :tone="urgencyTone(row.urgencyLevel)" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <StatusBadge :label="row.status" :tone="statusTone(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申领原因" min-width="240" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="detail(row.id)">详情</el-button>
              <el-button v-if="row.status === 'DRAFT'" size="small" type="success" @click="submit(row.id)">提交</el-button>
              <el-button v-if="row.status === 'SUBMITTED'" size="small" type="primary" @click="approve(row.id)">审批</el-button>
              <el-button v-if="row.status === 'SUBMITTED'" size="small" type="warning" @click="reject(row.id)">驳回</el-button>
              <el-button v-if="row.status === 'OUTBOUND'" size="small" @click="receive(row.id)">签收</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="AP" title="暂无申领单" description="创建申领单后，可以继续完成提交、审批和签收流转。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="createVisible" title="新建申领" eyebrow="Apply Workflow" subtitle="录入部门、紧急程度、申领原因和物资明细。" width="900">
      <el-form :model="createForm" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="部门ID">
          <el-input-number v-model="createForm.deptId" :min="1" />
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="createForm.urgencyLevel">
            <el-option :value="0" label="普通" />
            <el-option :value="1" label="一般" />
            <el-option :value="2" label="紧急" />
            <el-option :value="3" label="特急" />
          </el-select>
        </el-form-item>
        <el-form-item label="申领原因" style="grid-column: 1 / -1;">
          <el-input v-model="createForm.reason" type="textarea" />
        </el-form-item>
        <el-form-item label="使用场景" style="grid-column: 1 / -1;">
          <el-input v-model="createForm.scenario" />
        </el-form-item>
      </el-form>

      <div class="collection-editor">
        <div class="table-note">申领物资</div>
        <div v-for="(item, index) in createForm.items" :key="index" class="collection-editor__row collection-editor__row--4">
          <el-select v-model="item.materialId" placeholder="物资">
            <el-option v-for="material in materials" :key="material.id" :label="material.materialName" :value="material.id" />
          </el-select>
          <el-input-number v-model="item.applyQty" :min="1" />
          <div class="table-note">按当前物资库存和审批流转规则执行。</div>
          <div class="table-note">数量提交后由后端继续校验。</div>
          <el-button type="danger" @click="createForm.items.splice(index, 1)">删除</el-button>
        </div>
        <el-button @click="createForm.items.push({ materialId: null, applyQty: 1 })">添加物资</el-button>
      </div>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCreate">保存</el-button>
      </template>
    </DialogShell>

    <DialogShell v-model="detailVisible" title="申领详情" eyebrow="Apply Detail" subtitle="查看申领状态、物资清单与流转轨迹。" width="1080">
      <div class="surface-grid surface-grid--2">
        <DetailSection title="申领摘要" description="关键状态与物资内容。">
          <div class="detail-grid" v-if="detailData.order">
            <div class="detail-item">
              <span class="detail-item__label">状态</span>
              <div class="detail-item__value">
                <StatusBadge :label="detailData.order.status" :tone="statusTone(detailData.order.status)" />
              </div>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">紧急程度</span>
              <div class="detail-item__value">
                <StatusBadge :label="urgencyLabel(detailData.order.urgencyLevel)" :tone="urgencyTone(detailData.order.urgencyLevel)" />
              </div>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">申领原因</span>
              <span class="detail-item__value">{{ detailData.order.reason || '无' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">使用场景</span>
              <span class="detail-item__value">{{ detailData.order.scenario || '未填写' }}</span>
            </div>
          </div>

          <el-table :data="detailData.items || []" class="list-table">
            <el-table-column prop="materialId" label="物料ID" />
            <el-table-column prop="applyQty" label="申领数量" width="120" />
            <el-table-column prop="actualQty" label="实际数量" width="120" />
            <template #empty>
              <EmptyState compact glyph="IT" title="暂无物资明细" description="该申领单暂无可显示的物资项。" />
            </template>
          </el-table>
        </DetailSection>

        <DetailSection title="业务流转轨迹" description="节点状态、操作时间和操作人信息。">
          <DetailTimeline :items="timelineItems" :tone-resolver="timelineTone" />
        </DetailSection>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </DialogShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Bell, DataLine, Tickets, Warning } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../api'
import DetailSection from '../../components/ui/DetailSection.vue'
import DetailTimeline from '../../components/ui/DetailTimeline.vue'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const materials = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const createForm = reactive({ deptId: null, urgencyLevel: 0, reason: '', scenario: '', items: [{ materialId: null, applyQty: 1 }] })
const detailData = reactive({ order: null, items: [], timeline: [] })

const urgencyLabel = (level) => ['普通', '一般', '紧急', '特急'][Number(level) || 0] || '普通'
const urgencyTone = (level) => ({ 0: 'neutral', 1: 'accent', 2: 'warning', 3: 'danger' }[Number(level)] || 'neutral')
const statusTone = (status) => ({ DRAFT: 'neutral', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger', OUTBOUND: 'accent', RECEIVED: 'success' }[status] || 'neutral')
const timelineTone = item => ({ CREATE: 'accent', APPROVE: 'success', EXECUTE: 'teal', RECEIVE: 'success', REJECT: 'danger' }[item.badge] || 'neutral')

const metrics = computed(() => [
  { label: '申领总数', value: list.value.length, helper: '当前申领单总量', icon: Tickets, tone: 'accent' },
  { label: '待审批', value: list.value.filter(item => item.status === 'SUBMITTED').length, helper: '等待审批处理', icon: Warning, tone: 'warning' },
  { label: '高紧急度', value: list.value.filter(item => Number(item.urgencyLevel) >= 2).length, helper: '紧急与特急申领', icon: Bell, tone: 'danger' },
  { label: '已完成签收', value: list.value.filter(item => item.status === 'RECEIVED').length, helper: '已闭环单据', icon: DataLine, tone: 'success' }
])

const timelineItems = computed(() =>
  (detailData.timeline || []).map(item => ({
    title: item.detail,
    time: item.createdAt ? item.createdAt.replace('T', ' ') : '',
    meta: `操作人 ID: ${item.operatorId || '1'}`,
    badge: item.operation
  }))
)

const loadBase = async () => {
  materials.value = await apiGet('/api/material/info')
}

const load = async () => {
  list.value = await apiGet('/api/apply/list')
}

const openCreate = () => {
  createForm.deptId = null
  createForm.urgencyLevel = 0
  createForm.reason = ''
  createForm.scenario = ''
  createForm.items = [{ materialId: null, applyQty: 1 }]
  createVisible.value = true
}

const saveCreate = async () => {
  await apiPost('/api/apply', createForm)
  createVisible.value = false
  await load()
}

const detail = async (id) => {
  const [detailResponse, timelineResponse] = await Promise.all([
    apiGet(`/api/apply/${id}`),
    apiGet(`/api/apply/${id}/timeline`)
  ])
  Object.assign(detailData, detailResponse)
  detailData.timeline = timelineResponse || []
  detailVisible.value = true
}

const submit = async (id) => {
  await apiPost(`/api/apply/${id}/submit`)
  await load()
}

const approve = async (id) => {
  await apiPost(`/api/apply/${id}/approve`, { remark: '同意' })
  await load()
}

const reject = async (id) => {
  await apiPost(`/api/apply/${id}/reject`, { remark: '驳回' })
  await load()
}

const receive = async (id) => {
  await apiPost(`/api/apply/${id}/receive`)
  await load()
}

onMounted(async () => {
  await loadBase()
  await load()
})
</script>
