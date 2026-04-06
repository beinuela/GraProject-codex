<template>
  <div class="page-card">
    <h2 class="page-title">入库管理</h2>
    <el-button type="primary" @click="openCreate" style="margin-bottom:12px">新建入库单</el-button>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="warehouseId" label="仓库ID" width="90" />
      <el-table-column prop="sourceType" label="来源类型" />
      <el-table-column prop="operatorId" label="操作人ID" width="100" />
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
    </el-table>

    <el-dialog append-to-body v-model="visible" title="新建入库单" width="700">
      <el-form :model="form" label-width="100px">
        <el-form-item label="目标仓库">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width:100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="form.sourceType" style="width:100%">
            <el-option value="PURCHASE" label="采购入库" />
            <el-option value="TRANSFER" label="调拨入库" />
            <el-option value="OTHER" label="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
        <el-divider>入库明细</el-divider>
        <div v-for="(item, idx) in form.items" :key="idx" style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
          <el-select v-model="item.materialId" placeholder="物资" style="width:160px">
            <el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="width:120px" />
          <el-input v-model="item.batchNo" placeholder="批次号" style="width:140px" />
          <el-date-picker v-model="item.productionDate" type="date" placeholder="生产日期" value-format="YYYY-MM-DD" style="width:150px" />
          <el-date-picker v-model="item.expireDate" type="date" placeholder="过期日期" value-format="YYYY-MM-DD" style="width:150px" />
          <el-button type="danger" size="small" @click="form.items.splice(idx, 1)">删除</el-button>
        </div>
        <el-button @click="form.items.push({ materialId: null, quantity: 1, batchNo: '', productionDate: '', expireDate: '' })">添加明细</el-button>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">提交入库</el-button>
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
const form = reactive({ warehouseId: null, sourceType: 'PURCHASE', remark: '', items: [{ materialId: null, quantity: 1, batchNo: '', productionDate: '', expireDate: '' }] })

const loadBase = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
  materials.value = await apiGet('/api/material/info')
}
const load = async () => { list.value = await apiGet('/api/inventory/stock-in') }
const openCreate = () => {
  form.warehouseId = null; form.sourceType = 'PURCHASE'; form.remark = ''
  form.items = [{ materialId: null, quantity: 1, batchNo: '', productionDate: '', expireDate: '' }]
  visible.value = true
}
const save = async () => {
  await apiPost('/api/inventory/stock-in', form)
  ElMessage.success('入库成功')
  visible.value = false
  await load()
}

onMounted(async () => { await loadBase(); await load() })
</script>
