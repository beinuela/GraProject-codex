<template>
  <PageScaffold :metrics="metrics" page-type="workflow">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">调拨流程支持智能推荐调出仓，保留原有审批和执行接口。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新建调拨</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="调拨单列表" description="查看调拨状态、仓库去向与执行动作。" :badge="`${pagination.total} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fromWarehouseId" label="调出仓库" width="120" />
        <el-table-column prop="toWarehouseId" label="调入仓库" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <StatusBadge :label="row.status" :tone="statusTone(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="调拨原因" min-width="240" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" min-width="360" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="detail(row.id)">详情</el-button>
              <el-button v-if="row.status === 'DRAFT'" size="small" type="success" @click="submitOrder(row.id)">提交</el-button>
              <el-button v-if="row.status === 'SUBMITTED'" size="small" type="primary" @click="approveOrder(row.id)">审批</el-button>
              <el-button v-if="row.status === 'SUBMITTED'" size="small" type="warning" @click="rejectOrder(row.id)">驳回</el-button>
              <el-button v-if="row.status === 'APPROVED'" size="small" type="success" @click="executeOrder(row.id)">执行</el-button>
              <el-button v-if="row.status === 'OUTBOUND'" size="small" @click="receiveOrder(row.id)">签收</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="TR" title="暂无调拨单" description="创建调拨单后，可继续审批、执行和签收。" />
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

    <DialogShell v-model="createVisible" title="新建调拨" eyebrow="Transfer Workflow" subtitle="选择目标仓库、推荐调出仓并录入调拨物资。" width="980">
      <el-form :model="createForm" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="调入仓库">
          <el-select v-model="createForm.toWarehouseId" placeholder="所属校区 / 目标位置">
            <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.warehouseName + (warehouse.campus ? ` (${warehouse.campus})` : '')" :value="warehouse.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调出仓库">
          <div class="inline-actions" style="width: 100%;">
            <el-select v-model="createForm.fromWarehouseId" placeholder="可手动指定或智能推荐" style="flex: 1 1 240px;">
              <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.warehouseName" :value="warehouse.id" />
            </el-select>
            <el-button type="primary" plain @click="fetchRecommendation">智能推荐</el-button>
          </div>
        </el-form-item>
        <el-form-item label="调拨原因" style="grid-column: 1 / -1;">
          <el-input v-model="createForm.reason" type="textarea" />
        </el-form-item>
      </el-form>

      <DetailSection v-if="recommendations.length" title="推荐调度方案" description="基于目标校区与首个物资项，给出推荐调出仓。">
        <div class="stack-md">
          <div v-for="recommendation in recommendations" :key="recommendation.warehouseId" class="detail-item">
            <span class="detail-item__label">{{ recommendation.warehouseName }}</span>
            <span class="detail-item__value">预计路程 {{ recommendation.distance }} km，当前库存 {{ recommendation.availableQty }} 件。</span>
            <div class="inline-actions">
              <el-button size="small" type="primary" @click="applyRecommendation(recommendation.warehouseId)">应用此仓</el-button>
            </div>
          </div>
        </div>
      </DetailSection>

      <div class="collection-editor">
        <div class="table-note">调拨物资</div>
        <div v-for="(item, index) in createForm.items" :key="index" class="collection-editor__row collection-editor__row--4">
          <el-select v-model="item.materialId" placeholder="物资">
            <el-option v-for="material in materials" :key="material.id" :label="material.materialName" :value="material.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" />
          <div class="table-note">推荐逻辑仅参考第一项物资与数量。</div>
          <div class="table-note">最终库存校验仍由后端处理。</div>
          <el-button type="danger" @click="createForm.items.splice(index, 1)">删除</el-button>
        </div>
        <el-button @click="createForm.items.push({ materialId: null, quantity: 1 })">添加物资</el-button>
      </div>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCreate">提交申请</el-button>
      </template>
    </DialogShell>

    <DialogShell v-model="detailVisible" title="调拨详情" eyebrow="Transfer Detail" subtitle="查看调拨摘要和物资清单。" width="980">
      <div class="surface-grid surface-grid--2">
        <DetailSection title="调拨摘要" description="调拨状态、仓库方向与调拨原因。">
          <div class="detail-grid" v-if="detailData.order">
            <div class="detail-item">
              <span class="detail-item__label">状态</span>
              <div class="detail-item__value">
                <StatusBadge :label="detailData.order.status" :tone="statusTone(detailData.order.status)" />
              </div>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">调出仓库</span>
              <span class="detail-item__value">{{ detailData.order.fromWarehouseId }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">调入仓库</span>
              <span class="detail-item__value">{{ detailData.order.toWarehouseId }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">调拨原因</span>
              <span class="detail-item__value">{{ detailData.order.reason || '无' }}</span>
            </div>
          </div>
        </DetailSection>

        <DetailSection title="调拨物资" description="查看单据关联的物资与数量。">
          <el-table :data="detailData.items || []" class="list-table">
            <el-table-column prop="materialId" label="物资ID" />
            <el-table-column prop="quantity" label="数量" width="120" />
            <template #empty>
              <EmptyState compact glyph="IT" title="暂无物资明细" description="该调拨单暂无可显示的物资项。" />
            </template>
          </el-table>
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
import { ElMessage } from 'element-plus'
import { Bell, Check, Connection, Switch } from '@element-plus/icons-vue'
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
const warehouses = ref([])
const materials = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const createForm = reactive({ fromWarehouseId: null, toWarehouseId: null, reason: '', items: [{ materialId: null, quantity: 1 }] })
const detailData = reactive({ order: null, items: [] })
const recommendations = ref([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const statusTone = status => ({ DRAFT: 'neutral', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger', OUTBOUND: 'accent', RECEIVED: 'success' }[status] || 'neutral')

const metrics = computed(() => [
  { label: '调拨总数', value: pagination.total, helper: '当前调拨单总量', icon: Connection, tone: 'accent' },
  { label: '待审批', value: list.value.filter(item => item.status === 'SUBMITTED').length, helper: '等待审批的调拨单', icon: Bell, tone: 'warning' },
  { label: '执行中', value: list.value.filter(item => item.status === 'APPROVED' || item.status === 'OUTBOUND').length, helper: '已通过等待执行或签收', icon: Switch, tone: 'teal' },
  { label: '已签收', value: list.value.filter(item => item.status === 'RECEIVED').length, helper: '当前页已完成闭环', icon: Check, tone: 'success' }
])

const loadBase = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
  materials.value = await apiGet('/api/material/info')
}

const load = async () => {
  const result = await apiGet('/api/transfer/list', {
    page: pagination.page,
    size: pagination.size
  })
  list.value = result.records || []
  pagination.total = Number(result.total || 0)
}

const openCreate = () => {
  createForm.fromWarehouseId = null
  createForm.toWarehouseId = null
  createForm.reason = ''
  createForm.items = [{ materialId: null, quantity: 1 }]
  recommendations.value = []
  createVisible.value = true
}

const fetchRecommendation = async () => {
  if (!createForm.toWarehouseId) return ElMessage.warning('请先选择调入仓库')
  const firstItem = createForm.items[0]
  if (!firstItem || !firstItem.materialId || !firstItem.quantity) {
    return ElMessage.warning('请先指定至少一项调拨物资和数量')
  }

  const targetWarehouse = warehouses.value.find(item => item.id === createForm.toWarehouseId)
  if (!targetWarehouse || !targetWarehouse.campus) {
    return ElMessage.error('调入仓库缺乏校区地理信息')
  }

  const url = `/api/transfer/recommend?targetCampus=${encodeURIComponent(targetWarehouse.campus)}&materialId=${firstItem.materialId}&qty=${firstItem.quantity}`
  const result = await apiGet(url)
  recommendations.value = (result || []).slice(0, 3)
  if (!recommendations.value.length) {
    ElMessage.info('未找到符合条件的调出仓')
  }
}

const applyRecommendation = (warehouseId) => {
  createForm.fromWarehouseId = warehouseId
  recommendations.value = []
  ElMessage.success('已应用推荐调出仓')
}

const saveCreate = async () => {
  await apiPost('/api/transfer', createForm)
  createVisible.value = false
  pagination.page = 1
  await load()
}

const detail = async (id) => {
  const detailResponse = await apiGet(`/api/transfer/${id}`)
  Object.assign(detailData, detailResponse)
  detailVisible.value = true
}

const submitOrder = async (id) => {
  await apiPost(`/api/transfer/${id}/submit`)
  await load()
}

const approveOrder = async (id) => {
  await apiPost(`/api/transfer/${id}/approve`, { remark: '同意' })
  await load()
}

const rejectOrder = async (id) => {
  await apiPost(`/api/transfer/${id}/reject`, { remark: '驳回' })
  await load()
}

const executeOrder = async (id) => {
  await apiPost(`/api/transfer/${id}/execute`)
  await load()
}

const receiveOrder = async (id) => {
  await apiPost(`/api/transfer/${id}/receive`)
  await load()
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

onMounted(async () => {
  await loadBase()
  await load()
})
</script>
