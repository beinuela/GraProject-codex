<template>
  <div class="page-card">
    <h2 class="page-title">调拨管理</h2>
    <el-space style="margin-bottom:12px">
      <el-button type="primary" @click="openCreate">新建调拨</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="fromWarehouseId" label="调出仓库" width="100" />
      <el-table-column prop="toWarehouseId" label="调入仓库" width="100" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="reason" label="调拨原因" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="350">
        <template #default="scope">
          <el-space>
            <el-button size="small" @click="detail(scope.row.id)">详情</el-button>
            <el-button size="small" type="success" v-if="scope.row.status==='DRAFT'" @click="submitOrder(scope.row.id)">提交</el-button>
            <el-button size="small" type="primary" v-if="scope.row.status==='SUBMITTED'" @click="approveOrder(scope.row.id)">审批</el-button>
            <el-button size="small" type="warning" v-if="scope.row.status==='SUBMITTED'" @click="rejectOrder(scope.row.id)">驳回</el-button>
            <el-button size="small" type="success" v-if="scope.row.status==='APPROVED'" @click="executeOrder(scope.row.id)">执行</el-button>
            <el-button size="small" type="info" v-if="scope.row.status==='OUTBOUND'" @click="receiveOrder(scope.row.id)">签收</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建调拨 -->
    <el-dialog append-to-body v-model="createVisible" title="新建调拨 / Smart Dispatch" width="650">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="调入仓库">
          <el-select v-model="createForm.toWarehouseId" placeholder="应急事发地/缺货仓" style="width:100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName + ' (' + w.campus + ')'" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调出仓库">
          <div style="display:flex; width:100%; gap:10px;">
            <el-select v-model="createForm.fromWarehouseId" placeholder="可手动指定或智能推荐" style="flex:1">
              <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
            </el-select>
            <el-button color="#22d3ee" style="color:#0f172a" @click="fetchRecommendation">
               <el-icon><Guide /></el-icon> 智能推荐
            </el-button>
          </div>
        </el-form-item>
        
        <div v-if="recommendations.length > 0" style="margin-bottom: 20px; background: #e0f2fe; border: 1px solid #bae6fd; padding:15px; border-radius: 6px;">
          <h4 style="margin-top:0; color: #0369a1; display:flex; align-items:center; gap:5px;">
            <el-icon><Check /></el-icon> AI 调度推荐方案
          </h4>
          <div v-for="(rec, idx) in recommendations" :key="rec.warehouseId" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
            <div>
              <span style="font-weight:bold; color: #0c4a6e;">NO.{{idx+1}} {{ rec.warehouseName }} ({{ rec.campus }})</span>
              <br/>
              <span style="font-size:12px; color: #0284c7;">预计路程：{{ rec.distance }} km | 当前库存：{{ rec.availableQty }} 件</span>
            </div>
            <el-button size="small" type="primary" plain @click="applyRecommendation(rec.warehouseId)">应用此仓</el-button>
          </div>
        </div>

        <el-form-item label="调拨原因"><el-input v-model="createForm.reason" type="textarea" /></el-form-item>
        <el-divider>调拨物资</el-divider>
        <div v-for="(item, idx) in createForm.items" :key="idx" style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
          <el-select v-model="item.materialId" placeholder="物资" style="width:200px">
            <el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="width:140px" />
          <el-button type="danger" size="small" @click="createForm.items.splice(idx, 1)">删除</el-button>
        </div>
        <el-button @click="createForm.items.push({ materialId: null, quantity: 1 })">添加物资</el-button>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCreate">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog append-to-body v-model="detailVisible" title="调拨详情" width="600">
      <el-descriptions :column="2" border v-if="detailData.order">
        <el-descriptions-item label="ID">{{ detailData.order.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailData.order.status }}</el-descriptions-item>
        <el-descriptions-item label="调出仓库ID">{{ detailData.order.fromWarehouseId }}</el-descriptions-item>
        <el-descriptions-item label="调入仓库ID">{{ detailData.order.toWarehouseId }}</el-descriptions-item>
        <el-descriptions-item label="调拨原因">{{ detailData.order.reason }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detailData.items || []" border style="margin-top:12px">
        <el-table-column prop="materialId" label="物资ID" />
        <el-table-column prop="quantity" label="数量" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { apiGet, apiPost } from '../../api'
import { ElMessage } from 'element-plus'
import { Guide, Check } from '@element-plus/icons-vue'

const list = ref([])
const warehouses = ref([])
const materials = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const createForm = reactive({ fromWarehouseId: null, toWarehouseId: null, reason: '', items: [{ materialId: null, quantity: 1 }] })
const detailData = reactive({ order: null, items: [] })
const recommendations = ref([])

const loadBase = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
  materials.value = await apiGet('/api/material/info')
}
const load = async () => { list.value = await apiGet('/api/transfer/list') }

const openCreate = () => {
  createForm.fromWarehouseId = null; createForm.toWarehouseId = null; createForm.reason = ''
  createForm.items = [{ materialId: null, quantity: 1 }]
  recommendations.value = []
  createVisible.value = true
}

const fetchRecommendation = async () => {
  if (!createForm.toWarehouseId) return ElMessage.warning('请先选择调入仓库 (事发地)')
  const firstItem = createForm.items[0]
  if (!firstItem || !firstItem.materialId || !firstItem.quantity) {
    return ElMessage.warning('请先指定至少一项调拨物资和数量')
  }

  const targetWh = warehouses.value.find(w => w.id === createForm.toWarehouseId)
  if (!targetWh || !targetWh.campus) return ElMessage.error('调入仓库缺乏校区地理信息')

  try {
    const url = `/api/transfer/recommend?targetCampus=${encodeURIComponent(targetWh.campus)}&materialId=${firstItem.materialId}&qty=${firstItem.quantity}`
    const result = await apiGet(url)
    if (!result || result.length === 0) {
      ElMessage.info('未找到符合库存条件的调出仓')
      recommendations.value = []
    } else {
      recommendations.value = result.slice(0, 3) // Top 3
    }
  } catch (error) {
    console.error(error)
  }
}

const applyRecommendation = (whId) => {
  createForm.fromWarehouseId = whId
  recommendations.value = []
  ElMessage.success('已自动填入推荐的调出仓！')
}

const saveCreate = async () => { await apiPost('/api/transfer', createForm); createVisible.value = false; await load() }
const detail = async (id) => { const d = await apiGet(`/api/transfer/${id}`); Object.assign(detailData, d); detailVisible.value = true }
const submitOrder = async (id) => { await apiPost(`/api/transfer/${id}/submit`); await load() }
const approveOrder = async (id) => { await apiPost(`/api/transfer/${id}/approve`, { remark: '同意' }); await load() }
const rejectOrder = async (id) => { await apiPost(`/api/transfer/${id}/reject`, { remark: '驳回' }); await load() }
const executeOrder = async (id) => { await apiPost(`/api/transfer/${id}/execute`); await load() }
const receiveOrder = async (id) => { await apiPost(`/api/transfer/${id}/receive`); await load() }

onMounted(async () => { await loadBase(); await load() })
</script>
