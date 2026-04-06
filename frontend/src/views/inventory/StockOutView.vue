<template>
  <div class="page-card">
    <h2 class="page-title">出库管理</h2>
    <el-button type="primary" @click="openCreate" style="margin-bottom:12px">新建出库单</el-button>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="applyOrderId" label="申领单ID" width="100" />
      <el-table-column prop="warehouseId" label="仓库ID" width="90" />
      <el-table-column prop="operatorId" label="操作人ID" width="100" />
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
    </el-table>

    <el-dialog append-to-body v-model="visible" title="新建出库单" width="650">
      <el-form :model="form" label-width="100px">
        <el-form-item label="申领单ID"><el-input-number v-model="form.applyOrderId" :min="0" style="width:100%" placeholder="可选" /></el-form-item>
        <el-form-item label="出库仓库">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width:100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
        <el-divider>出库明细</el-divider>
        <div v-for="(item, idx) in form.items" :key="idx" style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
          <el-select v-model="item.materialId" placeholder="物资" style="width:200px">
            <el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="width:140px" />
          <el-button type="danger" size="small" @click="form.items.splice(idx, 1)">删除</el-button>
        </div>
        <el-button @click="form.items.push({ materialId: null, quantity: 1 })">添加明细</el-button>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">提交出库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet, apiPost } from '../../api'

const list = ref([])
const warehouses = ref([])
const materials = ref([])
const visible = ref(false)
const form = reactive({ applyOrderId: null, warehouseId: null, remark: '', items: [{ materialId: null, quantity: 1 }] })

const loadBase = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
  materials.value = await apiGet('/api/material/info')
}
const load = async () => { list.value = await apiGet('/api/inventory/stock-out') }
const openCreate = () => {
  form.applyOrderId = null; form.warehouseId = null; form.remark = ''
  form.items = [{ materialId: null, quantity: 1 }]
  visible.value = true
}
const save = async () => {
  await apiPost('/api/inventory/stock-out', form)
  ElMessage.success('出库成功')
  visible.value = false
  await load()
}

onMounted(async () => { await loadBase(); await load() })
</script>
