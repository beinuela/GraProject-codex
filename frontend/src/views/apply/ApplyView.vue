<template>
  <div class="page-card">
    <h2 class="page-title">申请审批</h2>
    <el-space wrap style="margin-bottom:12px">
      <el-button type="primary" @click="openCreate">新建申请</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="申请ID" width="90" />
      <el-table-column prop="deptId" label="申请部门ID" width="100" />
      <el-table-column prop="urgencyLevel" label="紧急程度" width="90" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="reason" label="申请原因" min-width="180" />
      <el-table-column label="操作" width="420">
        <template #default="scope">
          <el-space wrap>
            <el-button size="small" @click="detail(scope.row.id)">详情</el-button>
            <el-button size="small" type="primary" @click="submit(scope.row.id)">提交</el-button>
            <el-button size="small" type="success" @click="approve(scope.row.id)">通过</el-button>
            <el-button size="small" type="warning" @click="reject(scope.row.id)">驳回</el-button>
            <el-button size="small" type="info" @click="receive(scope.row.id)">签收</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="新建申请" width="760">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="申请部门">
              <el-select v-model="form.deptId" style="width:100%"><el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" /></el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="紧急程度"><el-input-number v-model="form.urgencyLevel" :min="0" :max="3" style="width:100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="申请原因"><el-input v-model="form.reason" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="使用场景"><el-input v-model="form.scenario" /></el-form-item></el-col>
        </el-row>
      </el-form>

      <el-table :data="form.items" border>
        <el-table-column label="物资" min-width="220">
          <template #default="scope">
            <el-select v-model="scope.row.materialId" filterable style="width:100%">
              <el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="申请数量" width="130">
          <template #default="scope"><el-input-number v-model="scope.row.applyQty" :min="1" style="width:100%" /></template>
        </el-table-column>
        <el-table-column label="操作" width="80"><template #default="scope"><el-button link type="danger" @click="form.items.splice(scope.$index,1)">删除</el-button></template></el-table-column>
      </el-table>
      <el-button style="margin-top:8px" @click="form.items.push({ materialId: null, applyQty: 1 })">新增明细</el-button>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="申请详情" size="55%">
      <pre>{{ JSON.stringify(currentDetail, null, 2) }}</pre>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { apiGet, apiPost } from '../../api'

const list = ref([])
const materials = ref([])
const depts = ref([])
const visible = ref(false)
const detailVisible = ref(false)
const currentDetail = ref({})

const form = reactive({ deptId: null, urgencyLevel: 0, reason: '', scenario: '', items: [{ materialId: null, applyQty: 1 }] })

const load = async () => { list.value = await apiGet('/api/apply/list') }
const loadBase = async () => {
  materials.value = await apiGet('/api/material/info')
  depts.value = await apiGet('/api/rbac/depts')
}

const openCreate = () => {
  Object.assign(form, { deptId: null, urgencyLevel: 0, reason: '', scenario: '', items: [{ materialId: null, applyQty: 1 }] })
  visible.value = true
}

const create = async () => {
  await apiPost('/api/apply', form)
  visible.value = false
  await load()
}

const submit = async (id) => { await apiPost(`/api/apply/${id}/submit`, {}); await load() }
const approve = async (id) => {
  const { value } = await ElMessageBox.prompt('请输入审批备注', '审批通过', { inputValue: '同意' })
  await apiPost(`/api/apply/${id}/approve`, {}, { remark: value })
  await load()
}
const reject = async (id) => {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '审批驳回', { inputValue: '材料不足' })
  await apiPost(`/api/apply/${id}/reject`, {}, { remark: value })
  await load()
}
const receive = async (id) => { await apiPost(`/api/apply/${id}/receive`, {}); await load() }

const detail = async (id) => {
  currentDetail.value = await apiGet(`/api/apply/${id}`)
  detailVisible.value = true
}

onMounted(async () => { await loadBase(); await load() })
</script>
