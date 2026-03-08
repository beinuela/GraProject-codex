<template>
  <div class="page-card">
    <h2 class="page-title">入库管理</h2>
    <el-form :model="form" inline>
      <el-form-item label="仓库">
        <el-select v-model="form.warehouseId" style="width:180px">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model="form.sourceType" placeholder="PURCHASE/DONATION" style="width:180px" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" style="width:220px" />
      </el-form-item>
    </el-form>

    <el-table :data="form.items" border style="margin-top:8px">
      <el-table-column label="物资" min-width="180">
        <template #default="scope">
          <el-select v-model="scope.row.materialId" filterable style="width:100%">
            <el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="批次号" min-width="140">
        <template #default="scope"><el-input v-model="scope.row.batchNo" /></template>
      </el-table-column>
      <el-table-column label="数量" width="120">
        <template #default="scope"><el-input-number v-model="scope.row.quantity" :min="1" style="width:100%" /></template>
      </el-table-column>
      <el-table-column label="生产日期" width="150">
        <template #default="scope"><el-date-picker v-model="scope.row.productionDate" value-format="YYYY-MM-DD" type="date" style="width:100%" /></template>
      </el-table-column>
      <el-table-column label="过期日期" width="150">
        <template #default="scope"><el-date-picker v-model="scope.row.expireDate" value-format="YYYY-MM-DD" type="date" style="width:100%" /></template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="scope"><el-button link type="danger" @click="remove(scope.$index)">删除</el-button></template>
      </el-table-column>
    </el-table>

    <el-space style="margin-top:12px">
      <el-button @click="add">新增明细</el-button>
      <el-button type="primary" @click="submit">提交入库</el-button>
    </el-space>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet, apiPost } from '../../api'

const warehouses = ref([])
const materials = ref([])

const form = reactive({
  warehouseId: null,
  sourceType: 'PURCHASE',
  remark: '',
  items: [{ materialId: null, batchNo: '', quantity: 1, productionDate: '', expireDate: '' }]
})

const loadBase = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
  materials.value = await apiGet('/api/material/info')
}

const add = () => form.items.push({ materialId: null, batchNo: '', quantity: 1, productionDate: '', expireDate: '' })
const remove = (i) => form.items.splice(i, 1)

const submit = async () => {
  if (!form.warehouseId || form.items.length === 0) {
    ElMessage.warning('请填写仓库和明细')
    return
  }
  await apiPost('/api/inventory/stock-in', form)
  ElMessage.success('入库完成')
  form.items = [{ materialId: null, batchNo: '', quantity: 1, productionDate: '', expireDate: '' }]
}

onMounted(loadBase)
</script>
