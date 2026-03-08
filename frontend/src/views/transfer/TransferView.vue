<template>
  <div class="page-card">
    <h2 class="page-title">调拨管理</h2>
    <el-space wrap style="margin-bottom:12px">
      <el-button type="primary" @click="openCreate">新建调拨</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="调拨ID" width="90" />
      <el-table-column prop="fromWarehouseId" label="调出仓库" width="100" />
      <el-table-column prop="toWarehouseId" label="调入仓库" width="100" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="reason" label="调拨原因" min-width="180" />
      <el-table-column label="操作" width="520">
        <template #default="scope">
          <el-space wrap>
            <el-button size="small" @click="detail(scope.row.id)">详情</el-button>
            <el-button size="small" type="primary" @click="submit(scope.row.id)">提交</el-button>
            <el-button size="small" type="success" @click="approve(scope.row.id)">通过</el-button>
            <el-button size="small" type="warning" @click="reject(scope.row.id)">驳回</el-button>
            <el-button size="small" type="danger" @click="execute(scope.row.id)">执行调拨</el-button>
            <el-button size="small" type="info" @click="receive(scope.row.id)">确认调入</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="新建调拨" width="760">
      <el-form :model="form" label-width="100px" inline>
        <el-form-item label="调出仓库">
          <el-select v-model="form.fromWarehouseId" style="width:200px"><el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" /></el-select>
        </el-form-item>
        <el-form-item label="调入仓库">
          <el-select v-model="form.toWarehouseId" style="width:200px"><el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" /></el-select>
        </el-form-item>
      </el-form>
      <el-form :model="form" label-width="100px">
        <el-form-item label="调拨原因"><el-input v-model="form.reason" /></el-form-item>
      </el-form>

      <el-table :data="form.items" border>
        <el-table-column label="物资" min-width="220">
          <template #default="scope"><el-select v-model="scope.row.materialId" filterable style="width:100%"><el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" /></el-select></template>
        </el-table-column>
        <el-table-column label="数量" width="120"><template #default="scope"><el-input-number v-model="scope.row.quantity" :min="1" style="width:100%" /></template></el-table-column>
        <el-table-column label="操作" width="80"><template #default="scope"><el-button link type="danger" @click="form.items.splice(scope.$index,1)">删除</el-button></template></el-table-column>
      </el-table>
      <el-button style="margin-top:8px" @click="form.items.push({ materialId: null, quantity: 1 })">新增明细</el-button>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="调拨详情" size="55%"><pre>{{ JSON.stringify(currentDetail, null, 2) }}</pre></el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { apiGet, apiPost } from '../../api'

const list = ref([])
const materials = ref([])
const warehouses = ref([])
const visible = ref(false)
const detailVisible = ref(false)
const currentDetail = ref({})

const form = reactive({ fromWarehouseId: null, toWarehouseId: null, reason: '', items: [{ materialId: null, quantity: 1 }] })

const load = async () => { list.value = await apiGet('/api/transfer/list') }
const loadBase = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
  materials.value = await apiGet('/api/material/info')
}

const openCreate = () => {
  Object.assign(form, { fromWarehouseId: null, toWarehouseId: null, reason: '', items: [{ materialId: null, quantity: 1 }] })
  visible.value = true
}
const create = async () => { await apiPost('/api/transfer', form); visible.value = false; await load() }
const submit = async (id) => { await apiPost(`/api/transfer/${id}/submit`, {}); await load() }
const approve = async (id) => {
  const { value } = await ElMessageBox.prompt('审批备注', '审批通过', { inputValue: '同意' })
  await apiPost(`/api/transfer/${id}/approve`, {}, { remark: value })
  await load()
}
const reject = async (id) => {
  const { value } = await ElMessageBox.prompt('驳回原因', '审批驳回', { inputValue: '库存不足' })
  await apiPost(`/api/transfer/${id}/reject`, {}, { remark: value })
  await load()
}
const execute = async (id) => { await apiPost(`/api/transfer/${id}/execute`, {}); await load() }
const receive = async (id) => { await apiPost(`/api/transfer/${id}/receive`, {}); await load() }
const detail = async (id) => { currentDetail.value = await apiGet(`/api/transfer/${id}`); detailVisible.value = true }

onMounted(async () => { await loadBase(); await load() })
</script>
