<template>
  <div class="page-card">
    <h2 class="page-title">申领审批</h2>
    <el-space style="margin-bottom:12px">
      <el-button type="primary" @click="openCreate">新建申领</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="deptId" label="部门ID" width="90" />
      <el-table-column prop="applicantId" label="申请人ID" width="100" />
      <el-table-column prop="urgencyLevel" label="紧急程度" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.urgencyLevel >= 2 ? 'danger' : 'info'" size="small">
            {{ ['普通','一般','紧急','特急'][scope.row.urgencyLevel] || '普通' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="reason" label="申领原因" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="300">
        <template #default="scope">
          <el-space>
            <el-button size="small" @click="detail(scope.row.id)">详情</el-button>
            <el-button size="small" type="success" v-if="scope.row.status==='DRAFT'" @click="submit(scope.row.id)">提交</el-button>
            <el-button size="small" type="primary" v-if="scope.row.status==='SUBMITTED'" @click="approve(scope.row.id)">审批</el-button>
            <el-button size="small" type="warning" v-if="scope.row.status==='SUBMITTED'" @click="reject(scope.row.id)">驳回</el-button>
            <el-button size="small" type="info" v-if="scope.row.status==='OUTBOUND'" @click="receive(scope.row.id)">签收</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建申领 -->
    <el-dialog append-to-body v-model="createVisible" title="新建申领" width="650">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="部门ID"><el-input-number v-model="createForm.deptId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="createForm.urgencyLevel" style="width:100%">
            <el-option :value="0" label="普通" />
            <el-option :value="1" label="一般" />
            <el-option :value="2" label="紧急" />
            <el-option :value="3" label="特急" />
          </el-select>
        </el-form-item>
        <el-form-item label="申领原因"><el-input v-model="createForm.reason" type="textarea" /></el-form-item>
        <el-form-item label="使用场景"><el-input v-model="createForm.scenario" /></el-form-item>
        <el-divider>申领物资</el-divider>
        <div v-for="(item, idx) in createForm.items" :key="idx" style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
          <el-select v-model="item.materialId" placeholder="物资" style="width:200px">
            <el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" />
          </el-select>
          <el-input-number v-model="item.applyQty" :min="1" placeholder="数量" style="width:140px" />
          <el-button type="danger" size="small" @click="createForm.items.splice(idx, 1)">删除</el-button>
        </div>
        <el-button @click="createForm.items.push({ materialId: null, applyQty: 1 })">添加物资</el-button>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog append-to-body v-model="detailVisible" title="申领详情与业务流转轨迹" width="700" custom-class="premium-dialog">
      <div class="premium-section">
        <h4 class="section-title">申领状态与物料</h4>
        <div style="margin-bottom: 15px;">
          <el-tag :type="detailData.order?.status === 'RECEIVED' ? 'success' : 'info'" effect="light" class="custom-status-tag">
            {{ detailData.order?.status?.toLowerCase() }}
          </el-tag>
        </div>
        <div style="font-size: 14px; color: #334155; margin-bottom: 5px;">
          <strong>状态：</strong>{{ detailData.order?.status === 'RECEIVED' ? '已签收' : '处理中' }}
        </div>
        <div style="font-size: 14px; color: #334155; margin-bottom: 15px;">
          <strong>申领原因：</strong>{{ detailData.order?.reason || '无' }}
        </div>

        <el-table :data="detailData.items || []" border style="width: 100%;" :header-cell-style="{background:'#f8fafc', color:'#475569', fontWeight:'bold'}">
          <el-table-column prop="materialId" label="物料ID" />
          <el-table-column prop="applyQty" label="申领数量" width="120" align="center" />
          <el-table-column prop="actualQty" label="实际数量" width="120" align="center" />
        </el-table>
      </div>

      <div class="premium-section" style="margin-top: 25px;">
        <h4 class="section-title">业务流转轨迹</h4>
        
        <div v-if="detailData.timeline && detailData.timeline.length > 0" class="timeline-wrapper">
          <el-timeline>
            <el-timeline-item
              v-for="(activity, index) in detailData.timeline"
              :key="index"
              :type="getTimelineType(activity.operation)"
              :icon="getTimelineIcon(activity.operation)"
              size="large"
              placement="top"
              hide-timestamp
            >
              <div class="timeline-content-row">
                <div class="op-badge" :class="'badge-' + getTimelineType(activity.operation)">{{ activity.operation }}</div>
                <div class="timeline-body">
                  <div class="tl-title">{{ activity.detail }}</div>
                  <div class="tl-meta">
                    <span>{{ activity.createdAt ? activity.createdAt.replace('T', ' ') : '' }}</span>
                    <el-divider direction="vertical" />
                    <span>操作人ID: {{ activity.operatorId || '1' }}</span>
                  </div>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
        <div v-else style="text-align: center; color: #cbd5e1; padding: 20px;">暂无流转记录</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { apiGet, apiPost } from '../../api'
import { Check, InfoFilled, Right, Edit, Select, Download, WarningFilled, Timer } from '@element-plus/icons-vue'

const list = ref([])
const materials = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const createForm = reactive({ deptId: null, urgencyLevel: 0, reason: '', scenario: '', items: [{ materialId: null, applyQty: 1 }] })
const detailData = reactive({ order: null, items: [], timeline: [] })

const getTimelineType = (op) => {
  if (op === 'CREATE') return 'info'
  if (op === 'APPROVE' || op === 'RECEIVE') return 'success'
  if (op === 'EXECUTE') return 'primary'
  if (op === 'REJECT') return 'danger'
  return 'info'
}

const getTimelineIcon = (op) => {
  if (op === 'CREATE') return InfoFilled
  if (op === 'APPROVE' || op === 'RECEIVE') return Check
  if (op === 'EXECUTE') return Right
  if (op === 'REJECT') return WarningFilled
  return Edit
}

const loadBase = async () => { materials.value = await apiGet('/api/material/info') }
const load = async () => { list.value = await apiGet('/api/apply/list') }

const openCreate = () => {
  createForm.deptId = null; createForm.urgencyLevel = 0; createForm.reason = ''; createForm.scenario = ''
  createForm.items = [{ materialId: null, applyQty: 1 }]
  createVisible.value = true
}

const saveCreate = async () => { await apiPost('/api/apply', createForm); createVisible.value = false; await load() }

const detail = async (id) => { 
  const d = await apiGet(`/api/apply/${id}`)
  const t = await apiGet(`/api/apply/${id}/timeline`)
  Object.assign(detailData, d)
  detailData.timeline = t || []
  detailVisible.value = true 
}

const submit = async (id) => { await apiPost(`/api/apply/${id}/submit`); await load() }
const approve = async (id) => { await apiPost(`/api/apply/${id}/approve`, { remark: '同意' }); await load() }
const reject = async (id) => { await apiPost(`/api/apply/${id}/reject`, { remark: '驳回' }); await load() }
const receive = async (id) => { await apiPost(`/api/apply/${id}/receive`); await load() }

onMounted(async () => { await loadBase(); await load() })
</script>

<style scoped>
.premium-section {
  background: #ffffff;
  border-radius: 8px;
  padding: 10px 5px;
}
.section-title {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 16px;
  color: #1e293b;
  font-weight: bold;
}
.custom-status-tag {
  border-radius: 12px;
  padding: 0 12px;
  font-weight: 500;
  text-transform: uppercase;
  font-size: 12px;
}
.timeline-wrapper {
  padding-left: 10px;
  margin-top: 20px;
}
.timeline-content-row {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  margin-top: -3px;
}
.op-badge {
  font-size: 12px;
  font-weight: bold;
  padding: 2px 0;
  width: 70px;
  text-align: left;
}
.badge-info { color: #64748b; }
.badge-success { color: #10b981; }
.badge-primary { color: #3b82f6; }
.badge-danger { color: #ef4444; }

.timeline-body {
  flex: 1;
}
.tl-title {
  font-weight: bold;
  color: #0ea5e9;
  font-size: 14px;
  margin-bottom: 4px;
}
.tl-meta {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
}
:deep(.el-timeline-item__tail) {
  border-left: 2px solid #e2e8f0;
}
:deep(.el-timeline-item__node--large) {
  width: 20px;
  height: 20px;
  left: -4px;
}
</style>
