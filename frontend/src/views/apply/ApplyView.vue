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
    <el-dialog v-model="createVisible" title="新建申领" width="650">
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
    <el-dialog v-model="detailVisible" title="申领详情" width="600">
      <el-descriptions :column="2" border v-if="detailData.order">
        <el-descriptions-item label="ID">{{ detailData.order.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailData.order.status }}</el-descriptions-item>
        <el-descriptions-item label="申领原因">{{ detailData.order.reason }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">{{ detailData.order.urgencyLevel }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detailData.items || []" border style="margin-top:12px">
        <el-table-column prop="materialId" label="物资ID" />
        <el-table-column prop="applyQty" label="申领数量" />
        <el-table-column prop="actualQty" label="实发数量" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { apiGet, apiPost } from '../../api'

const list = ref([])
const materials = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const createForm = reactive({ deptId: null, urgencyLevel: 0, reason: '', scenario: '', items: [{ materialId: null, applyQty: 1 }] })
const detailData = reactive({ order: null, items: [] })

const loadBase = async () => { materials.value = await apiGet('/api/material/info') }
const load = async () => { list.value = await apiGet('/api/apply/list') }
const openCreate = () => {
  createForm.deptId = null; createForm.urgencyLevel = 0; createForm.reason = ''; createForm.scenario = ''
  createForm.items = [{ materialId: null, applyQty: 1 }]
  createVisible.value = true
}
const saveCreate = async () => { await apiPost('/api/apply', createForm); createVisible.value = false; await load() }
const detail = async (id) => { const d = await apiGet(`/api/apply/${id}`); Object.assign(detailData, d); detailVisible.value = true }
const submit = async (id) => { await apiPost(`/api/apply/${id}/submit`); await load() }
const approve = async (id) => { await apiPost(`/api/apply/${id}/approve`, { remark: '同意' }); await load() }
const reject = async (id) => { await apiPost(`/api/apply/${id}/reject`, { remark: '驳回' }); await load() }
const receive = async (id) => { await apiPost(`/api/apply/${id}/receive`); await load() }

onMounted(async () => { await loadBase(); await load() })
</script>
